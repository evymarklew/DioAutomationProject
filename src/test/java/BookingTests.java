import Entities.User;
import Entities.BookingDates;
import Entities.Booking;
import Entities.Auth;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.*;

public class BookingTests {
    public static Faker faker;

    public static RequestSpecification request;

    //public static Booking booking;

    //public static BookingDates bookingdates;

    public static Auth auth;

    public static Auth authError;

    public static User user;

    public static Booking booking;

    public static BookingDates bookingDates;

    @BeforeAll
    public static void Setup(){
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
        faker = new Faker();
        user = new User(faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                faker.internet().safeEmailAddress(),
                faker.internet().password(8,10),
                faker.phoneNumber().toString());

        auth = new Auth("admin", "password123");
        authError = new Auth("admin", "password");
        bookingDates = new BookingDates("2018-01-02", "2018-01-03");
        booking = new Booking(user.getFirstname(),
                user.getLastname(),
                (float)faker.number().randomDouble(2,50, 1000000),
                true,
                bookingDates,
                "");
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(), new ErrorLoggingFilter());
    }

    @BeforeEach
    void setRequest(){
        request = given().config(RestAssured.config().
                logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails())).
                contentType(ContentType.JSON).
                auth().basic("admin", "password123");
    }

    @Test
    public void getTokenCreation_returnOK(){
        Auth test = auth;
        given().config(RestAssured.config()
                .logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .when()
                .body(auth)
                .post("/auth")
                .then()
                .assertThat()
                .statusCode(200);
    }
    @Test
    public void getTokenCreation_returnFailed(){
        given().config(RestAssured.config()
                        .logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .contentType(ContentType.JSON)
                .when()
                .body(authError)
                .post("/auth")
                .then()
                .assertThat()
                .statusCode(200);
    }

    @Test
    public void getBookingById_returnOK(){
        Response response = request.when().get("/booking/1").then().extract().response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200,response.statusCode());
    }
    @Test
    public void getAllBookingsById_returnOK(){
        Response response = request.when().get("/booking").then().extract().response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200,response.statusCode());

    }
    @Test
    public void getAllBookingsByUserFirstName_BookingExists_returnOK(){
        request
                .when()
                .queryParam("firstName","Evelyn")
                .get("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body("results", hasSize(greaterThan(0)));
    }

    @Test
    public void createBooking_WithValidData_returnOK(){
        Booking test = booking;
        request
                .when()
                .body(booking)
                .post("/booking")
                .then()
                .body(matchesJsonSchemaInClasspath("createBookingRequestSchema.json"))
                .and()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .time(lessThan(2000L));
    }

    @Test
    public void updateBookingById_returnOK() {
        Number random = faker.number().randomNumber(3,false);
        String putURI = String.format("/booking/%d", random);

        given()
                .auth().basic("admin", "password123")
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
                .when()
                .body(booking)
                .put(putURI)
                .then()
                .body(matchesJsonSchemaInClasspath("updateBookingResponseSchema.json"))
                .and()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .time(lessThan(2000L));
    }

    @Test
    public void deleteBookingById_returnOK() {
        Number random = 3;
        String putURI = String.format("/booking/%d", random);
        given()
                .auth().basic("admin", "password123")
                .when()
                .delete(putURI)
                .then()
                .assertThat()
                .statusCode(200);
    }

    @Test
    public void deleteBookingById_failed_noAuth() {
        Number random = 3;
        String putURI = String.format("/booking/%d", random);
        given()
                .auth().basic("admin", "password123")
                .when()
                .delete(putURI)
                .then()
                .assertThat()
                .statusCode(403);
    }
}