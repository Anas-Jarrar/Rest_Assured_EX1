package org.example;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;

import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginTest {

    private static String sessionValue;
    private static String listId;
    private static String randomName;
    private static String randomDescription;
    private static String[] seeds;
    private static String seedEditionTitle;
    private static String sizeBeforeCreateNewList;
    private static String theOffset;
    private static String seedId;
    private static String seedSubjectTitle;
    private static String addedSeedKey;

    @BeforeAll
    public static void login() {
        RestAssured.baseURI = "https://openlibrary.org";
        Response loginResponse = given()
                .header("Content-Type", "application/json")
                .body("{\"access\": \"QeohAvKlojuy3x1o\", \"secret\": \"w4MpBultBO3HnM8y\"}")
                .when()
                .post("/account/login.json")
                .then()
                .statusCode(200)
                .extract().response();

        String cookieHeader = loginResponse.getHeader("Set-Cookie");
        assertNotNull(cookieHeader, "Set-Cookie header is missing");
        //session=/people/anasjarrar%2C2025-09-20T19%3A55%3A29%2C25839%244387a2458fc8813b04c6f928ea8fe23e; Path=/
        if (cookieHeader.contains("session=")) {
            sessionValue = cookieHeader.split(";")[0];
        }
        assertNotNull(sessionValue, "Session cookie not found");

        System.out.println("Extracted session cookie: " + sessionValue);

        Random random = new Random();
        String[] names = {"Ahmad", "Sara", "Omar", "Lina", "Hadi"};
        String[] desc = {"AA", "BB", "CC", "DD", "EE", "FF", "GG"};

        randomName = names[random.nextInt(names.length)]
                + names[random.nextInt(names.length)]
                + random.nextInt(10000);

        randomDescription = desc[random.nextInt(desc.length)]
                + random.nextInt(10000);
        seeds = new String[]{"/books/OL1M", "/subjects/gothic_architecture"};
        System.out.println("Random Name: " + randomName);
        System.out.println("Random Description: " + randomDescription);
    }

    @Test
    @Order(1)
    public void retrieveAllList() {
        Response responseBody = given()
                .header("Content-Type", "application/json")
                .when()
                .get("/people/anasjarrar/lists.json")
                .then()
                .statusCode(200)
                .extract()
                .response();
        assertNotNull(responseBody.jsonPath().getList("entries"), "Entries array should be exist");
        List<?> entries = responseBody.jsonPath().getList("entries");
        assertFalse(entries.isEmpty(), "Entries should not be empty");
        sizeBeforeCreateNewList = responseBody.jsonPath().getString("size");
        theOffset = responseBody.jsonPath().getString("size");
    }

    @Test
    @Order(2)
    public void createList() {
        String seedsJson = Arrays.toString(seeds)
                .replace("[", "[\"")
                .replace("]", "\"]")
                .replace(", ", "\", \"");
        Response createListResponse = given()
                .header("Content-Type", "application/json")
                .header("Cookie", sessionValue)
                .body("{\"name\":\"" + randomName + "\","
                        + "\"description\":\"" + randomDescription + "\","
                        + "\"seeds\": " + seedsJson + "}")
                .when()
                .post("/people/anasjarrar/lists")
                .then()
                .statusCode(200)
                .extract().response();
        String listKey = createListResponse.jsonPath().getString("key");
        assertNotNull(listKey, "List key not found in the response");
        String[] theKey = listKey.split("/");
        listId = theKey[theKey.length - 1];
        System.out.println("Created list ID: " + listId);
        System.out.println("Created list seed: " + seedsJson);
    }

    @Test
    @Order(3)
    public void retrieveAllListForCheckSizeAndNewList() {
        Response responseBody = given()
                .header("Content-Type", "application/json")
                .when()
                .queryParam("offset", theOffset)
                .queryParam("limit", 10)
                .get("/people/anasjarrar/lists.json")
                .then()
                .statusCode(200)
                .extract()
                .response();
        assertEquals(Integer.parseInt(sizeBeforeCreateNewList) + 1, Integer.parseInt(responseBody.jsonPath().getString("size")));
        assertTrue(responseBody.jsonPath().getString("entries.url").contains(listId), "Entries array should contains the new list Id");
        System.out.println("the list of URls : " + responseBody.jsonPath().getList("entries.url"));
    }

    @Test
    @Order(4)
    public void getCreatedList() {
        Response getListCreated = given()
                .header("Content-Type", "application/json")
                .when()
                .get("https://openlibrary.org/people/anasjarrar/lists/" + listId + ".json")
                .then()
                .statusCode(200)
                .extract().response();
        String theKey = getListCreated.jsonPath().getString("type.key");
        String[] theKeys = theKey.split("/");
        String theNewListId = theKeys[theKeys.length - 1];
        assertEquals(listId, theNewListId, "New list ID should be the created list ID");
        String listName = getListCreated.jsonPath().getString("name");
        String listDescription = getListCreated.jsonPath().getString("description");
        assertEquals(randomName, listName, "List name does not match");
        assertEquals(randomDescription, listDescription, "List description does not match");

        System.out.println("List name: " + listName + "\nList description: " + listDescription);
    }

    @Test
    @Order(5)
    public void checkSeedsOfList() {
        Response getSeedOfList = given()
                .header("Content-Type", "application/json")
                .when()
                .get("/people/anasjarrar/lists/" + listId + "/seeds.json")
                .then()
                .statusCode(200)
                .extract().response();

        List<?> entries = getSeedOfList.jsonPath().getList("entries");
        assertNotNull(entries, "Seeds response must have 'entries'");
        assertFalse(entries.isEmpty(), "Seeds entries should not be empty");

        List<String> responseUrls = getSeedOfList.jsonPath().getList("entries.url");
        System.out.println("Response URLs: " + responseUrls);
        for (String seed : seeds) {
            assertTrue(responseUrls.contains(seed),
                    "Expected seed not found in response: " + seed);
        }
        List<String> allTitles = getSeedOfList.jsonPath().getList("entries.title");
        System.out.println("All Titles: " + allTitles);
        seedEditionTitle = allTitles.get(0);
        List<String> Urls = getSeedOfList.jsonPath().getList("entries.url");
        String[] theUrl = Urls.get(0).split("/");
        seedId = theUrl[2];
        seedSubjectTitle = Urls.get(1);
    }

    @Test
    @Order(6)
    public void checkEditionsOfList() {
        Response getEditionOfList = given()
                .header("Content-Type", "application/json")
                .when()
                .get("/people/anasjarrar/lists/" + listId + "/editions.json")
                .then()
                .statusCode(200)
                .extract().response();

        List<String> editionTitle = getEditionOfList.jsonPath().getList("entries.title");
        String expectedSeedEditionTitle = editionTitle.get(0);
        assertEquals(seedEditionTitle, expectedSeedEditionTitle, "List edition title does not match");
        System.out.println("seedEditionTitle: " + seedEditionTitle);
        System.out.println("expectedSeedEditionTitle:" + expectedSeedEditionTitle);
    }

    @Test
    @Order(7)
    public void checkSubjectOfList() {
        Response getSubjectOfList = given()
                .header("Content-Type", "application/json")
                .when()
                .get("/people/anasjarrar/lists/" + listId + "/subjects.json")
                .then()
                .statusCode(200)
                .extract().response();
        List<String> subjectUrls = getSubjectOfList.jsonPath().getList("subjects.url");
        boolean found = subjectUrls.stream().anyMatch(subjectUrl -> subjectUrl.equals(seedSubjectTitle));
        assertTrue(found, "Seed subject title was not found in the list");
    }

    @Test
    @Order(8)
    public void listContainingSeed() {
        Response response = given()
                .header("Content-Type", "application/json")
                .when()
                .get("/books/" + seedId + "/lists.json")
                .then()
                .statusCode(200)
                .extract().response();

        List<?> entries = response.jsonPath().getList("entries");
        assertNotNull(entries, "Seeds response must have 'entries'");
        assertFalse(entries.isEmpty(), "Seeds entries should not be empty");

        List<String> entriesURls=response.jsonPath().getList("entries.url");
        boolean found =entriesURls.stream().anyMatch(entry -> entry.contains(listId));

        assertTrue(found, "Seed URL not found in the list");
    }


    @Test
    @Order(9)
    public void addSeedAndVerify() {
        addedSeedKey = "/books/OL25123431M";
        given()
                .header("Content-Type", "application/json")
                .header("Cookie", sessionValue)
                .body("{\"add\":[{\"key\":\"" + addedSeedKey + "\"}]}")
                .when()
                .post("/people/anasjarrar/lists/" + listId + "/seeds")
                .then()
                .statusCode(200)
                .extract().response();


        Response getSeedOfList = given()
                .header("Content-Type", "application/json")
                .when()
                .get("/people/anasjarrar/lists/" + listId + "/seeds.json")
                .then()
                .statusCode(200)
                .extract().response();

        List<String> responseUrls = getSeedOfList.jsonPath().getList("entries.url");
        boolean found = responseUrls.stream()
                .anyMatch(seedUrl -> seedUrl.equals(addedSeedKey));
        assertTrue(found, "The seed was not added successfully");
    }


    @Test
    @Order(10)
    public void deleteSeedAndVerify() {
        addedSeedKey = "/books/OL25123431M";
                 given()
                .header("Content-Type", "application/json")
                .header("Cookie", sessionValue)
                .body("{\"remove\":[\"" + addedSeedKey + "\"]}")
                .when()
                .post("/people/anasjarrar/lists/" + listId + "/seeds")
                .then()
                .statusCode(200)
                .extract().response();

        Response getSeedOfList = given()
                .header("Content-Type", "application/json")
                .when()
                .get("/people/anasjarrar/lists/" + listId + "/seeds.json")
                .then()
                .statusCode(200)
                .extract().response();

        List<String> responseUrls = getSeedOfList.jsonPath().getList("entries.url");
        assertNotNull(responseUrls, "Seeds list should not be null");
        assertFalse(responseUrls.contains(addedSeedKey),
                "The seed was not deleted successfully");

    }

    @Test
    @Order(11)
    public void deleteListAndVerify() {
        given()
                .header("Content-Type", "application/json")
                .header("Cookie", sessionValue)
                .when()
                .post("/people/anasjarrar/lists/" + listId + "/delete.json")
                .then()
                .statusCode(200);
        Response getDeletedList = given()
                .header("Content-Type", "application/json")
                .when()
                .get("/people/anasjarrar/lists/" + listId + ".json")
                .then()
                .statusCode(200)
                .extract().response();
        assertNull(getDeletedList.jsonPath().getString("name"), "The list still found");
        assertNull(getDeletedList.jsonPath().getString("description"), "The list still found");
        assertNull(getDeletedList.jsonPath().getString("seed_count"), "The list still found");
        System.out.println("List " + listId + " deleted successfully.");
    }

    @Test
    @Order(12)
    public void searchListsWithLimitAndOffset() {
        String query = "book";
        int limit = 100;
        int offset = 30;
        Response response = given()
                .queryParam("q", query)
                .queryParam("limit", limit)
                .queryParam("offset", offset)
                .header("Content-Type", "application/json")
                .when()
                .get("/search/lists.json")
                .then()
                .statusCode(200)
                .extract()
                .response();

        List<?> docs = response.jsonPath().getList("docs");
        int start = response.jsonPath().getInt("start");
        assertNotNull(docs, "Docs should not be null");
        assertTrue(docs.size() <= limit, "Docs count should be at most " + limit);
        assertEquals(offset, start, "Start index does not match offset");
        System.out.println("Docs size: " + docs.size());
        System.out.println("Start index: " + start);
    }

}
