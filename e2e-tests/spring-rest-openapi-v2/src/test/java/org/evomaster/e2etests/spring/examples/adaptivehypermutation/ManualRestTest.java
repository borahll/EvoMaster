package org.evomaster.e2etests.spring.examples.adaptivehypermutation;


import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.List;
import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.db.dsl.SqlDsl.sql;
import static org.hamcrest.Matchers.equalTo;


/**
 * created by manzh on 2020/10/23
 */
public class ManualRestTest extends AHypermuationTestBase{

    @Test
    public void test_example(){
        List<InsertionDto> insertions = sql().insertInto("BAR", 0L)
                .d("A", "0")
                .d("B", "\"bar\"")
                .d("C", "369")
                .and().insertInto("FOO", 1L)
                .d("X", "1")
                .d("Y", "\"foo\"")
                .d("ZC", "10")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 2L)
                .d("X", "2")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 3L)
                .d("X", "3")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 4L)
                .d("X", "4")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 5L)
                .d("X", "5")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 6L)
                .d("X", "6")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 7L)
                .d("X", "7")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 8L)
                .d("X", "8")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 9L)
                .d("X", "9")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 10L)
                .d("X", "10")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 11L)
                .d("X", "11")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 12L)
                .d("X", "12")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 13L)
                .d("X", "13")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 14L)
                .d("X", "14")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 15L)
                .d("X", "15")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 16L)
                .d("X", "16")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 17L)
                .d("X", "17")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 18L)
                .d("X", "18")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 19L)
                .d("X", "19")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 20L)
                .d("X", "20")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 21L)
                .d("X", "21")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .dtos();
        controller.execInsertionsIntoDatabase(insertions);

        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 300.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2020-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/22?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B3B4B5"));

        given().accept("*/*")
                .get(baseUrlOfSut + "/api/bars/0")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.JSON)
                .body("'b'", equalTo("bar"))
                .body("'c'",equalTo(369));
    }

    @Test
    public void test_foo_B0(){
        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 1.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2019-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/1?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B0"));
    }

    @Test
    public void test_foo_B1(){
        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 100.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2019-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/1?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B1"));
    }

    @Test
    public void test_foo_B2(){
        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 200.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2019-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/1?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B2"));
    }

    @Test
    public void test_foo_B3(){
        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 300.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2019-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/1?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B3"));
    }

    @Test
    public void test_foo_B4(){
        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 300.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2020-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/1?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B3B4"));
    }

    @Test
    public void test_foo_B5(){
        List<InsertionDto> insertions = sql().insertInto("FOO", 1L)
                .d("X", "1")
                .d("Y", "\"foo\"")
                .d("ZC", "10")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 2L)
                .d("X", "2")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 3L)
                .d("X", "3")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 4L)
                .d("X", "4")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 5L)
                .d("X", "5")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 6L)
                .d("X", "6")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 7L)
                .d("X", "7")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 8L)
                .d("X", "8")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 9L)
                .d("X", "9")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 10L)
                .d("X", "10")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 11L)
                .d("X", "11")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 12L)
                .d("X", "12")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 13L)
                .d("X", "13")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 14L)
                .d("X", "14")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 15L)
                .d("X", "15")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 16L)
                .d("X", "16")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 17L)
                .d("X", "17")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 18L)
                .d("X", "18")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 19L)
                .d("X", "19")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 20L)
                .d("X", "20")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .and().insertInto("FOO", 21L)
                .d("X", "21")
                .d("Y", "\"foo\"")
                .d("ZC", "0")
                .d("ZT", "\"1900-01-20\"")
                .dtos();
        controller.execInsertionsIntoDatabase(insertions);

        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 300.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2020-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/22?y=foo")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(ContentType.TEXT)
                .body(equalTo("B3B4B5"));
    }

    @Test
    public void test_foo_bad_x(){
        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 1.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"evomaster_48_input\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/-1?y=foo")
                .then()
                .assertThat()
                .statusCode(400);
    }

    @Test
    public void test_foo_bad_y(){
        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 1.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"2010-01-01\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/1?y=bar")
                .then()
                .assertThat()
                .statusCode(400);
    }

    @Test
    public void test_foo_bad_zt(){
        given().accept("*/*")
                .contentType("application/json")
                .body(" { " +
                        " \"c\": 1.0, " +
                        " \"d1\": \"d1\", " +
                        " \"d2\": \"d2\", " +
                        " \"d3\": \"d3\", " +
                        " \"t\": \"evomaster_48_input\" " +
                        " } ")
                .post(baseUrlOfSut + "/api/foos/1?y=foo")
                .then()
                .assertThat()
                .statusCode(400);
    }
}
