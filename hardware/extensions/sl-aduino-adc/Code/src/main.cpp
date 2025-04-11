#include <Arduino.h>

void setup() {
  Serial.begin(9600); // initialize serial communication at 9600 bits per second
  pinMode(A0, INPUT); //pin of the data of the color sensor
  pinMode(13, OUTPUT); //Green LED pin for for different colors
  pinMode(12, OUTPUT); //Red LED pin
  pinMode(11, OUTPUT); //Blue LED pin
  pinMode(10, OUTPUT); //White LED pin
}

void loop() {
  int analogValue = 0; // variable to store the voltage value received from the sensor after the voltage divider
  int onTime = 3000;  // time in milliseconds for the LED to stay on
  int sensTime = 175; // time in milliseconds the color must be detected by the sensor for it to be considered as true
  int digitalValue = 0;
  int sensorPin = A0; //inti the sensor pin as A0
  float initialVoltage = 0.0; //true voltage value of the sensor (without the voltage divider)
  unsigned long conditionStartTime = 0; // time when the condition was met
  bool conditionMet = false; // flag to check if the condition was met

  digitalValue = analogRead(sensorPin); // read the digital value from the sensor
  Serial.print("Digital Value: ");
  Serial.print(digitalValue);

  analogValue = digitalValue * (5.0 / 1023.0);
  Serial.print(", Analog Value: ");
  Serial.print(analogValue);
  Serial.println(" V");

  initialVoltage = analogValue * 2.0;
  Serial.print(", Initial Voltage: ");
  Serial.print(initialVoltage);
  Serial.println(" V");

  while (digitalValue > 810 || digitalValue < 780) { // Check if the sensor is not detecting any color
    digitalValue = analogRead(sensorPin); // read the digital value from the sensor
    printf("green");  
    digitalWrite(13, HIGH); // Green LED on and every other leds off
    digitalWrite(12, LOW);
    digitalWrite(11, LOW);
    digitalWrite(10, LOW);
    if (digitalValue >= 720 && digitalValue <= 757) { // check if the value of the sensor is in the blue span for more than sensTime milliseconds
      if (!conditionMet) {
        conditionStartTime = millis();
        conditionMet = true;
      } else if (millis() - conditionStartTime >= sensTime) {
        printf("temps Blue = ", millis() - conditionStartTime);
        // Condition met for at least sensTime * milliseconds
        digitalWrite(12, LOW);
        digitalWrite(11, HIGH); // Blue LED on and every other leds off
        digitalWrite(10, LOW);
        delay(onTime); // delay for the LED to stay on
      }
    } else if (digitalValue >= 650 && digitalValue <= 710) { // check if the value of the sensor is in the red span for more than sensTime milliseconds
      if (!conditionMet) {
        conditionStartTime = millis();
        conditionMet = true;
      } else if (millis() - conditionStartTime >= sensTime) {
        printf("temps Red = ", millis() - conditionStartTime);
        // Condition met for at least sensTime * milliseconds
        digitalWrite(12, HIGH); // Red LED on and every other leds off
        digitalWrite(11, LOW);
        digitalWrite(10, LOW);
        delay(onTime); // delay for the LED to stay on
      }
    } else if (digitalValue >= 295 && digitalValue <= 420) { // check if the value of the sensor is in the white span for more than sensTime milliseconds
      if (!conditionMet) {
        conditionStartTime = millis();
        conditionMet = true;
      } else if (millis() - conditionStartTime >= sensTime) {
        printf("temps White = ", millis() - conditionStartTime);
        // Condition met for at least sensTime * milliseconds
        digitalWrite(12, LOW);
        digitalWrite(11, LOW);
        digitalWrite(10, HIGH); // White LED on and every other leds off
        delay(onTime); // delay for the LED to stay on
      }
    } else { // another color is detected
      conditionMet = false;
    }
  }
  digitalWrite(13, LOW);
  digitalWrite(12, LOW);
  digitalWrite(11, LOW);
  digitalWrite(10, LOW);
}