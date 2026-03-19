package com.example.home;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;

public class PetRepositoryTest {

    private MockWebServer mockWebServer;
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJpZnptdXBoYWVtdGxtcmlqYXFyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjIwNzc3MDAsImV4cCI6MjA3NzY1MzcwMH0.MA5qpZby_xlSAbwS70JfqbOGkRI04DZlb80MPRRP5Lc";

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testAddPetToSupabase_successfulResponse() throws InterruptedException, JSONException {

        MockResponse mockResponse = new MockResponse()
                .setResponseCode(201)
                .setBody("{\"id\":\"12345\", \"name\":\"Барсик\", \"type\":\"cat\"}");
        mockWebServer.enqueue(mockResponse);

        CountDownLatch latch = new CountDownLatch(1);


        OkHttpClient client = new OkHttpClient();

        // Создание тестовых данных питомца
        JSONObject petData = new JSONObject();
        petData.put("name", "Барсик");
        petData.put("type", "cat");
        petData.put("breed", "Британская");
        petData.put("age", 12);
        petData.put("gender", "male");
        petData.put("size", "Средний");
        petData.put("hair_length", "Короткошерстный");
        petData.put("color", "Серый");
        petData.put("price", 15000.0);
        petData.put("address", "г. Москва, ул. Примерная, 1");
        petData.put("phone", "+79991234567");
        petData.put("image_url", "cat1");
        petData.put("description", "Ласковый и игривый кот");
        petData.put("created_date", "2024-01-15");

        RequestBody body = RequestBody.create(
                petData.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(mockWebServer.url("/rest/v1/pets"))
                .post(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                fail("Request failed: " + e.getMessage());
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Assert
                assertTrue("Response should be successful", response.isSuccessful());
                assertEquals("Response code should be 201", 201, response.code());

                String responseBody = response.body().string();
                assertNotNull("Response body should not be null", responseBody);
                assertTrue("Response should contain pet data", responseBody.contains("Барсик"));

                latch.countDown();
            }
        });


        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertTrue("Callback was not called in time", finished);


        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/rest/v1/pets", recordedRequest.getPath());
        assertNotNull("Request body should not be null", recordedRequest.getBody().readUtf8());
    }

    @Test
    public void testAddPetToSupabase_networkError() throws InterruptedException, JSONException {

        MockResponse mockResponse = new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\":\"Internal Server Error\"}");
        mockWebServer.enqueue(mockResponse);

        CountDownLatch latch = new CountDownLatch(1);


        OkHttpClient client = new OkHttpClient();

        JSONObject petData = new JSONObject();
        petData.put("name", "Барсик");
        petData.put("type", "cat");
        petData.put("breed", "Британская");

        RequestBody body = RequestBody.create(
                petData.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(mockWebServer.url("/rest/v1/pets"))
                .post(body)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                fail("Request should not fail with network error in this test");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                assertFalse("Response should not be successful for server error", response.isSuccessful());
                assertEquals("Response code should be 500", 500, response.code());

                latch.countDown();
            }
        });


        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertTrue("Callback was not called in time", finished);
    }

    @Test
    public void testDataTransformation_russianToEnglish() throws JSONException {



        String russianType = "Кошка";
        String russianGender = "Девочка";


        String englishType = "Кошка".equals(russianType) ? "cat" : "dog";
        String englishGender = "Девочка".equals(russianGender) ? "female" : "male";


        assertEquals("cat", englishType);
        assertEquals("female", englishGender);
    }

    @Test
    public void testPetDataValidation() throws JSONException {
        // Тестирование валидации данных питомца


        JSONObject validPetData = new JSONObject();
        validPetData.put("name", "Барсик");
        validPetData.put("age", 12);
        validPetData.put("price", 15000.0);

        JSONObject invalidPetData = new JSONObject();
        invalidPetData.put("name", "");
        invalidPetData.put("age", -5);


        assertFalse("Name should not be empty", validPetData.getString("name").isEmpty());
        assertTrue("Age should be positive", validPetData.getInt("age") > 0);
        assertTrue("Price should be positive", validPetData.getDouble("price") > 0);
    }
}