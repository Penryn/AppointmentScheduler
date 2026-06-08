// 测试说明：验证登录页面、管理员导航和客户预约流程的真实浏览器交互。
package com.example.slabiak.appointmentscheduler.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
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
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.servlet.session.cookie.secure=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = LoginPageIT.Initializer.class)
@ActiveProfiles("integration-test")
public class LoginPageIT {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(30);
    private static final DockerImageName SELENIUM_CHROMIUM_IMAGE = DockerImageName
            .parse("selenium/standalone-chromium:4.44.0-20260505")
            .asCompatibleSubstituteFor("selenium/standalone-chrome");
    private static final File SELENIUM_RECORDINGS_DIRECTORY = Path.of("target", "selenium-recordings").toFile();

    @LocalServerPort
    private int port;

    @RegisterExtension
    private final SeleniumContainerExtension browser = new SeleniumContainerExtension();

    @Test
    public void shouldShowLoginPageAndSuccessfullyLoginToAdminAccountUsingAdminCredentials() {
        RemoteWebDriver driver = browser.getDriver();
        driver.manage().window().setSize(new Dimension(1280, 800));
        String url = "http://host.testcontainers.internal:" + port + "/";
        driver.get(url);

        // 检查点：登录页渲染了预期的登录表单。
        WebElement elementById = driver.findElement(By.id("login-form"));
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("qwerty123");
        driver.findElement(By.tagName("button")).click();

        // 检查点：管理员登录成功后可以看到预约入口。
        WebElement appointments = waitForClickable(driver, By.cssSelector("a[href='/appointments/all']"));

        assertNotNull(elementById);
        assertNotNull(appointments);
    }

    @Test
    public void shouldLoginAsRetailCustomerAndSuccessfullyBookNewAppointment() {
        RemoteWebDriver driver = browser.getDriver();
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
        navigateToFirstAvailableAppointmentSlot(driver);

        waitForClickable(driver, By.cssSelector("form[action='/appointments/new'] button[type='submit']")).click();

        // 检查点：客户完成预约后回到预约列表页。
        WebElement table = waitForElement(driver, By.id("appointments"));
        WebElement tableBody = table.findElement(By.tagName("tbody"));
        int rowCount = tableBody.findElements(By.tagName("tr")).size();
        // 检查点：列表中只有刚刚创建的一条预约记录。
        assertEquals(1, rowCount);
    }

    @Test
    public void shouldLoginAsAdminAndOpenCoreManagementLists() {
        RemoteWebDriver driver = browser.getDriver();
        driver.manage().window().setSize(new Dimension(1280, 800));
        String url = "http://host.testcontainers.internal:" + port + "/";

        driver.get(url);

        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).sendKeys("qwerty123");
        driver.findElement(By.tagName("button")).click();

        waitForClickable(driver, By.cssSelector("a[href='/customers/all']")).click();
        // 检查点：管理员可以打开客户管理列表。
        assertNotNull(waitForElement(driver, By.id("customers")));

        driver.get("http://host.testcontainers.internal:" + port + "/providers/all");
        // 检查点：管理员可以打开服务提供者管理列表。
        assertNotNull(waitForElement(driver, By.id("providers")));

        driver.get("http://host.testcontainers.internal:" + port + "/works/all");
        // 检查点：管理员可以打开服务项目管理列表。
        assertNotNull(waitForElement(driver, By.id("works")));
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

    private void navigateToFirstAvailableAppointmentSlot(RemoteWebDriver driver) {
        waitForElement(driver, By.id("calendar"));

        Object bookingPath = ((JavascriptExecutor) driver).executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                const calendar = document.getElementById('calendar');
                const availabilityBaseUrl = calendar.dataset.availabilityBaseUrl;
                const bookingBaseUrl = calendar.dataset.bookingBaseUrl;

                function formatDate(date) {
                    const month = String(date.getMonth() + 1).padStart(2, '0');
                    const day = String(date.getDate()).padStart(2, '0');
                    return `${date.getFullYear()}-${month}-${day}`;
                }

                (async function () {
                    const firstBookableDate = new Date();
                    firstBookableDate.setDate(firstBookableDate.getDate() + 1);

                    for (let dayOffset = 0; dayOffset < 30; dayOffset++) {
                        const candidateDate = new Date(firstBookableDate);
                        candidateDate.setDate(firstBookableDate.getDate() + dayOffset);

                        const response = await fetch(availabilityBaseUrl + formatDate(candidateDate), {
                            headers: {'X-Requested-With': 'XMLHttpRequest'}
                        });
                        if (!response.ok) {
                            continue;
                        }

                        const entries = await response.json();
                        if (entries.length > 0) {
                            done(bookingBaseUrl + encodeURIComponent(entries[0].start));
                            return;
                        }
                    }

                    done(null);
                })().catch(function (error) {
                    done('ERROR: ' + error.message);
                });
                """);

        if (bookingPath == null) {
            fail("No available appointment slot found in the next 30 calendar days");
        }
        if (bookingPath instanceof String path && path.startsWith("ERROR: ")) {
            fail(path);
        }

        driver.get("http://host.testcontainers.internal:" + port + bookingPath);
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

    private static void captureScreenshot(RemoteWebDriver driver, ExtensionContext context) {
        try {
            byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
            Path screenshotDirectory = Path.of("target", "screenshots");
            Files.createDirectories(screenshotDirectory);
            Files.write(screenshotDirectory.resolve(screenshotFileName(context)), screenshot);
        } catch (RuntimeException | IOException screenshotError) {
            System.err.println("Unable to capture Selenium failure screenshot: " + screenshotError.getMessage());
        }
    }

    private static String screenshotFileName(ExtensionContext context) {
        String rawName = context.getRequiredTestClass().getName() + "-" + context.getRequiredTestMethod().getName();
        return rawName.replaceAll("[^A-Za-z0-9._-]", "_") + ".png";
    }

    private static class SeleniumContainerExtension implements BeforeEachCallback, AfterEachCallback, TestExecutionExceptionHandler {

        private BrowserWebDriverContainer chrome;
        private Throwable testFailure;

        @Override
        public void beforeEach(ExtensionContext context) {
            testFailure = null;
            ensureRecordingDirectoryExists();
            chrome = new BrowserWebDriverContainer(SELENIUM_CHROMIUM_IMAGE)
                    .withCapabilities(new ChromeOptions())
                    .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL,
                            SELENIUM_RECORDINGS_DIRECTORY);
            chrome.start();
        }

        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
            testFailure = throwable;
            if (chrome != null && chrome.isRunning()) {
                captureScreenshot(chrome.getWebDriver(), context);
            }
            throw throwable;
        }

        @Override
        public void afterEach(ExtensionContext context) {
            if (chrome != null) {
                chrome.afterTest(testDescription(context), Optional.ofNullable(testFailure));
                chrome.stop();
            }
        }

        private RemoteWebDriver getDriver() {
            return chrome.getWebDriver();
        }

        private org.testcontainers.lifecycle.TestDescription testDescription(ExtensionContext context) {
            return new org.testcontainers.lifecycle.TestDescription() {
                @Override
                public String getTestId() {
                    return context.getUniqueId();
                }

                @Override
                public String getFilesystemFriendlyName() {
                    String rawName = context.getRequiredTestClass().getName() + "-"
                            + context.getRequiredTestMethod().getName();
                    return rawName.replaceAll("[^A-Za-z0-9._-]", "_");
                }
            };
        }

        private void ensureRecordingDirectoryExists() {
            try {
                Files.createDirectories(SELENIUM_RECORDINGS_DIRECTORY.toPath());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create Selenium recordings directory", e);
            }
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
