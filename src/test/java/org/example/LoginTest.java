package org.example;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

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
    private static String seedEditionTitle;

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

        if (cookieHeader.contains("session=")) {
            sessionValue = cookieHeader.split("session=")[1].split(";")[0];
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

        System.out.println("Random Name: " + randomName);
        System.out.println("Random Description: " + randomDescription);
    }

    @Test
    @Order(1)
    public void createList() {
        Response createListResponse = given()
                .header("Content-Type", "application/json")
                .header("Cookie", "session=" + sessionValue)
                .body("{\"name\":\"" + randomName + "\","
                        + "\"description\":\"" + randomDescription + "\","
                        + "\"seeds\": [\"/books/OL1M\", \"/subjects/gothic_architecture\"]}")
                .when()
                .post("/people/anasjarrar/lists")
                .then()
                .statusCode(200)
                .extract().response();
        String listKey = createListResponse.jsonPath().getString("key");
        assertNotNull(listKey, "List key not found in response");

        listId = listKey.substring(listKey.lastIndexOf("/") + 1);
        System.out.println("Created list ID: " + listId);
    }

    @Test
    @Order(2)
    public void getCreatedList() {
        Response getListCreated = given()
                .header("Content-Type", "application/json")
                .when()
                .get("https://openlibrary.org/people/anasjarrar/lists/" + listId + ".json")
                .then()
                .statusCode(200)
                .extract().response();

        String listName = getListCreated.jsonPath().getString("name");
        String listDescription = getListCreated.jsonPath().getString("description");

        assertNotNull(listName, "List name not found in response");
        assertNotNull(listDescription, "List description not found in response");

        assertEquals(randomName, listName, "List name does not match");
        assertEquals(randomDescription, listDescription, "List description does not match");

        System.out.println("List name: " + listName + "\nList description: " + listDescription);
    }

    @Test
    @Order(3)
    public void checkSeedsOfList() {
        Response getSeedOfList=given()
                .header("Content-Type", "application/json")
                .when()
                .get("https://openlibrary.org/people/anasjarrar/lists/" + listId + "/seeds.json")
                .then()
                .statusCode(200)
                .extract().response();
        List<String> allTitles = getSeedOfList.jsonPath().getList("entries.title");
        System.out.println("All Titles: " + allTitles);
        seedEditionTitle = allTitles.get(0);
        System.out.println("First Title: " + seedEditionTitle);
        System.out.println("Seeds of list: " + listId);
    }
    @Test
    @Order(4)
    public void checkEditionsOfList() {
        Response getEditionOfList=given()
                .header("Content-Type", "application/json")
                .when()
                .get("https://openlibrary.org/people/anasjarrar/lists/" + listId + "/editions.json")
                .then()
                .statusCode(200)
                .extract().response();

        List<String> editionTitle=getEditionOfList.jsonPath().getList("entries.title");
        String expectedSeedEditionTitle=editionTitle.get(0);
        assertEquals(seedEditionTitle, expectedSeedEditionTitle, "List edition title does not match");
        System.out.println("seedEditionTitle: " + seedEditionTitle);
        System.out.println("expectedSeedEditionTitle:" + expectedSeedEditionTitle);
    }
    @Test
    @Order(5)
    public void checkSubjectOfList() {
        given()
                .header("Content-Type", "application/json")
                .when()
                .get("https://openlibrary.org/people/anasjarrar/lists/" + listId + "/subjects.json")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(6)
    public void deleteList() {
        given()
                .header("Content-Type", "application/json")
                .header("Cookie", "session=" + sessionValue)
                .when()
                .post("/people/anasjarrar/lists/" + listId + "/delete.json")
                .then()
                .statusCode(200);

        System.out.println("List " + listId + " deleted successfully.");
    }
}
