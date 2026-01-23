import pytest
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


BASE_URL = "https://programsko-inzenjerstvo-csfw.onrender.com/login"


@pytest.fixture
def driver():
    driver = webdriver.Chrome()
    driver.maximize_window()
    yield driver
    driver.quit()


def test_successful_login(driver):
    driver.get(BASE_URL)

    wait = WebDriverWait(driver, 10)

    email_input = wait.until(
        EC.presence_of_element_located((By.CSS_SELECTOR, "input[placeholder='Email']"))
    )
    password_input = driver.find_element(By.CSS_SELECTOR, "input[placeholder='Password']")
    submit_btn = driver.find_element(By.CLASS_NAME, "submit-btn")

    email_input.send_keys("admin@admin")
    password_input.send_keys("admin")
    submit_btn.click()

    wait.until(EC.url_contains("/questions"))

    driver.save_screenshot("tc01_successful_login.png")

    assert "/questions" in driver.current_url

def test_empty_fields(driver):
    driver.get(BASE_URL)

    wait = WebDriverWait(driver, 10)

    submit_btn = wait.until(
        EC.presence_of_element_located((By.CLASS_NAME, "submit-btn"))
    )
    submit_btn.click()

    assert "/login" in driver.current_url

    driver.save_screenshot("tc02_empty_fields.png")

def test_wrong_password(driver):
    driver.get(BASE_URL)

    wait = WebDriverWait(driver, 10)

    driver.find_element(By.CSS_SELECTOR, "input[placeholder='Email']").send_keys("user@fer.hr")
    driver.find_element(By.CSS_SELECTOR, "input[placeholder='Password']").send_keys("krivalozinka")
    driver.find_element(By.CLASS_NAME, "submit-btn").click()

    error_message = wait.until(
        EC.presence_of_element_located((By.CLASS_NAME, "error"))
    )

    driver.save_screenshot("tc03_wrong_password.png")

    assert "Incorrect Password" in error_message.text

def test_redirect_to_login_when_not_authenticated(driver):
    driver.get("https://programsko-inzenjerstvo-csfw.onrender.com")

    driver.execute_script("window.localStorage.clear();")

    driver.get("https://programsko-inzenjerstvo-csfw.onrender.com/home")

    wait = WebDriverWait(driver, 10)

    wait.until(EC.url_contains("/login"))

    driver.save_screenshot("tc04_redirect_not_logged_in.png")

    assert "/login" in driver.current_url
