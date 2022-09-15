package com.example.exercise;

import com.example.exercise.model.InputModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Random;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InputTests {

    @Autowired
    private RedissonClient redisson;
    @Autowired
    private MockMvc mockMvc;
    private static final String CHARACTERS = "ACGT";
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final Random random = new Random();
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    private void beforeEach() {
        redisson.getKeys().flushall();
        System.setOut(new PrintStream(outputStream));
    }


    @Test
    public void test_single_input() throws Exception {
        Integer userId = 1234;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < 512; ++i) {
            buffer.append('G');
        }

        sendRequest(userId, buffer.toString());
        Thread.sleep(6000);
        assertTrue(outputStream.toString().contains("number of G : 512"));
    }

    @Test
    public void test_multiple_input() throws Exception {
        Integer userId = 1234;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < 100; ++i) {
            buffer.append('G');
        }


        sendRequest(userId, buffer.toString());
        sendRequest(userId, buffer.toString());
        sendRequest(userId, buffer.toString());
        sendRequest(userId, buffer.toString());
        sendRequest(userId, buffer.toString());
        sendRequest(userId, buffer.toString());

        Thread.sleep(6000);
        assertTrue(outputStream.toString().contains("number of G : 512"));
    }

    @Test
    public void negative_test_multiple_input() throws Exception {
        Integer userId = 1234;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < 100; ++i) {
            buffer.append('Z');
        }


        sendRequest(userId, buffer.toString());
        sendRequest(userId, buffer.toString());
        sendRequest(userId, buffer.toString());
        sendRequest(userId, buffer.toString());
        sendRequest(userId, buffer.toString());
        sendRequest(userId, buffer.toString());

        Thread.sleep(6000);
        assertFalse(outputStream.toString().contains("number of G : 512"));
    }

    @Test
    public void random_test_multiple_user_multiple_input() throws Exception {
        Integer userId = 1234;
        Integer userId2 = 3412;

        int answerForUserOne = 0;
        int answerForUserTwo = 0;

        String temp = generateRandomString(200);
        answerForUserOne += countG(temp);
        sendRequest(userId, temp);

        temp = generateRandomString(100);
        answerForUserTwo += countG(temp);
        sendRequest(userId2, temp);

        temp = generateRandomString(300);
        answerForUserOne += countG(temp);
        sendRequest(userId, temp);

        temp = generateRandomString(200);
        answerForUserTwo += countG(temp);
        sendRequest(userId2, temp);

        temp = generateRandomString(12);
        answerForUserOne += countG(temp);
        sendRequest(userId, temp);


        Thread.sleep(10000);
        assertTrue(outputStream.toString().contains("number of G : " + answerForUserOne));

        temp = generateRandomString(212);
        answerForUserTwo += countG(temp);
        sendRequest(userId2, temp);

        Thread.sleep(6000);
        assertTrue(outputStream.toString().contains("number of G : " + answerForUserTwo));
    }


    private void sendRequest(Integer userId, String input) throws Exception {
        InputModel inputModel = new InputModel(input);
        mockMvc.perform(post("/accept")
                        .content(objectMapper.writeValueAsString(inputModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("USER_ID", userId))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    private String generateRandomString(int size) {
        StringBuffer buffer = new StringBuffer("");
        for (int i = 0; i < size; ++i) {
            buffer.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return buffer.toString();
    }

    private int countG(String temp) {
        int answer = 0;
        for (int i = 0; i < temp.length(); ++i) {
            if (temp.charAt(i) == 'G') ++answer;
        }
        return answer;
    }
}
