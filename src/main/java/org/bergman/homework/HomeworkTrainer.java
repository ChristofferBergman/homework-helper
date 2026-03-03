package org.bergman.homework;

import java.util.Map;
import java.util.Scanner;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Query;

public class HomeworkTrainer {
	private static final String QUERY_SETUP =
			"""
			MATCH (q:Question)
			REMOVE q.alreadyAsked
			""";
	
	private static final String QUERY_GET_QUESTION =
			"""
			MATCH (q:Question) WHERE q.alreadyAsked IS NULL
			WITH q ORDER BY rand() LIMIT 1
			SET q.alreadyAsked = TRUE
			RETURN q.question AS question
			""";
	private static final String QUERY_CHECK_ANSWER =
			"""
			CYPHER 25
			MATCH (q:Question {question: $question})-[:ANSWER]->(a:Answer)-[:__NODE_TO_CHUNK__]->(c:__Chunk__)
			CALL(a, c) {
			  WHEN a.answer = $answer THEN
			  RETURN 3 AS score, "Spot on" AS evaluation, a.answer AS correct
			  ELSE
			  WITH ai.text.structuredCompletion(
			    "Make an evaluation of how good this answer is to this question. " +
			    "Return 0 if the answer is deemed to be incorrect. " +
			    "If it is correct give it a score between 1 and 3 depending on how good it was. " +
			    "Give it 1 if it answers the bare minimum and 3 if it is well developed and covers all parts the answer.\n" +
			    "This is the question: " + $question + "\n" +
			    "This is the answer provided (to be graded): " + $answer + "\n" +
			    "This is the correct answer: " + a.answer + "\n" +
			    "Here is more context on the subject: " + c.text,
			    {
			      type: 'object',
			      properties: {
			        score: {
			          type: 'integer',
			          description: '0 if answer is incorrect, and 1-3 for how good the answer is if it is correct.',
			          minimum: 0,
			          maximum: 3
			        },
			        evaluation: {
			          type: 'string',
			          description: 'An explanation of why that score was given.'
			        }
			      },
			      required: ['score', 'evaluation'],
			      additionalProperties: false
			    },
			    "OpenAI",
			    {token: $apiKey, model: "gpt-5.2"}) AS result
			    RETURN result.score AS score, result.evaluation AS evaluation, a.answer AS correct
			}
			RETURN score, evaluation, correct
			""";

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.err.println("Too few arguments.");
			System.err.println("Usage:");
			System.err.println("HomeworkTrainer DB_URL DB_NAME USER PASSWORD API_KEY");
			return;
		}
		
		String url = args[0];
		String db = args[1];
		String user = args[2];
		String pwd = args[3];
		String apiKey = args[4];
		
		Driver driver = null;
		Scanner scanner = new Scanner(System.in);
		try {
			driver = GraphDatabase.driver(url, AuthTokens.basic(user, pwd), Config.defaultConfig());
			
			// Initialize a new round
			/*try (var session = driver.session(SessionConfig.forDatabase(db))) {
				session.executeWriteWithoutResult(tx -> tx.run(new Query(QUERY_SETUP)).consume());
			}*/
			
			// Ask all available questions until we are out of questions to ask
			int questions = 0;
			int correctAnswers = 0;
			int totalScore = 0;
			while (true) {
				try (var session = driver.session(SessionConfig.forDatabase(db))) {
					var records = session.executeWrite(tx ->
						tx.run(new Query(QUERY_GET_QUESTION)).list());
					if (records.isEmpty()) {
						System.out.println("Exam over");
						System.out.println("You got " + correctAnswers + " of " + questions);
						if (correctAnswers > 0) {
							System.out.println("Your average score (1-3) of the correct answers were: " +
									String.format("%.2f", (double)totalScore/correctAnswers));
						}
						break;
					}
					var question = records.get(0).get("question").asString();
					
					System.out.println("Question " + (++questions));
					System.out.println(question);
					String answer = scanner.nextLine();
					
					var result = session.executeWrite(tx ->
						tx.run(new Query(QUERY_CHECK_ANSWER,
								Map.of("question", question, "answer", answer, "apiKey", apiKey)))
						.single());
					var score = result.get("score").asInt();
					var evaluation = result.get("evaluation").asString();
					var correct = result.get("correct").asString();
					if (score == 0) {
						System.out.println("Incorrect answer!");
						System.out.println("Correct answer: " + correct);
						System.out.println("Evaluation: " + evaluation);
					} else  {
						System.out.println("Correct answer! Good job!");
						System.out.println("Score: " + score);
						System.out.println("Correct answer: " + correct);
						System.out.println("Evaluation: " + evaluation);
						totalScore += score;
						correctAnswers++;
					}
					System.out.println();
				}
			}
		}
		finally {
			if (driver != null) {
				driver.close();
			}
			scanner.close();
		}
	}

}
