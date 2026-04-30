package com.example.slabiak.appointmentscheduler.ui;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = LoginPageIT.Initializer.class)
@ActiveProfiles("integration-test")
public class LoginPageIT {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(30);

    @LocalServerPort
    private int port;

    @Rule
    public BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
            .withCapabilities(new ChromeOptions());

    @Test
    public void shouldShowLoginPageAndSuccessfullyLoginToAdminAccountUsingAdminCredentials() {
        RemoteWebDriver driver = chrome.getWebDriver();
        driver.manage().window().setSize(new Dimension(1280, 800));
        String url = "http://host.testcontainers.internal:" + port + "/";
        driver.get(url);

        WebElement elementById = driver.findElement(By.id("login-form"));
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("qwerty123");
        driver.findElement(By.tagName("button")).click();

        WebElement appointments = waitForClickable(driver, By.cssSelector("a[href='/appointments/all']"));

        assertNotNull(elementById);
        assertNotNull(appointments);
    }

    @Test
    public void shouldLoginAsRetailCustomerAndSuccessfullyBookNewAppointment() {
        RemoteWebDriver driver = chrome.getWebDriver();
        driver.manage().window().setSize(new Dimension(1280, 800));
        String url = "http://host.testcontainers.internal:" + port + "/";

        driver.get(url);

        driver.findElement(By.id("username")).sendKeys("customer_r");
        driver.findElement(By.id("password")).sendKeys("qwerty123");
        driver.findElement(By.tagName("button")).click();

        waitForClickable(driver, By.cssSelector("a[href='/appointments/all']")).click();
        waitForClickable(driver, By.cssSelector("a[href='/appointments/new']")).click();
        waitForClickable(driver, By.cssSelector("a.btn-primary[href^='/appointments/new/']")).click();
        waitForClickable(driver, By.cssSelector("a.btn-primary[href^='/appointments/new/']")).click();
        clickFirstAvailableAppointmentSlot(driver);

        waitForClickable(driver, By.cssSelector("form[action='/appointments/new'] button[type='submit']")).click();

        WebElement table = driver.findElement(By.id("appointments"));
        WebElement tableBody = table.findElement(By.tagName("tbody"));
        int rowCount = tableBody.findElements(By.tagName("tr")).size();
        assertEquals(1, rowCount);
    }

    private WebElement waitForClickable(RemoteWebDriver driver, By locator) {
        Instant deadline = Instant.now().plus(WAIT_TIMEOUT);
        RuntimeException lastError = null;

        while (Instant.now().isBefore(deadline)) {
            try {
                WebElement element = driver.findElement(locator);
                if (element.isDisplayed() && element.isEnabled()) {
                    return element;
                }
            } catch (RuntimeException e) {
                lastError = e;
            }
            sleepBriefly();
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new NoSuchElementException("Timed out waiting for clickable element: " + locator);
    }

    private void clickFirstAvailableAppointmentSlot(RemoteWebDriver driver) {
        waitForElement(driver, By.id("calendar"));

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            List<WebElement> slots = driver.findElements(By.cssSelector("#calendar a.fc-event[href]"));
            if (!slots.isEmpty()) {
                slots.get(0).click();
                return;
            }

            waitForClickable(driver, By.cssSelector("#calendar .fc-next-button")).click();
            waitForElement(driver, By.id("calendar"));
            sleepBriefly();
        }

        fail("No available appointment slot found in the next seven calendar days");
    }

    private WebElement waitForElement(RemoteWebDriver driver, By locator) {
        Instant deadline = Instant.now().plus(WAIT_TIMEOUT);
        NoSuchElementException lastError = null;

        while (Instant.now().isBefore(deadline)) {
            try {
                return driver.findElement(locator);
            } catch (NoSuchElementException e) {
                lastError = e;
            }
            sleepBriefly();
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new NoSuchElementException("Timed out waiting for element: " + locator);
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Selenium element", e);
        }
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            applicationContext.addApplicationListener((ApplicationListener<WebServerInitializedEvent>) event -> {
                Testcontainers.exposeHostPorts(event.getWebServer().getPort());
            });
        }
    }
}
