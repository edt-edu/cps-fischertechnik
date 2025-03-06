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
  int analogValue = 0;
  int onTime = 3000;
  int sensTime = 175;
  int digitalValue = 0;
  int sensorPin = A0;
  float initialVoltage = 0.0;
  unsigned long conditionStartTime = 0;
  bool conditionMet = false;

  digitalValue = analogRead(sensorPin);
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

  while (digitalValue > 810 || digitalValue < 780) {
    digitalValue = analogRead(sensorPin);
    printf("green");  
    digitalWrite(13, HIGH); // Green LED on
    digitalWrite(12, LOW);
    digitalWrite(11, LOW);
    digitalWrite(10, LOW);
    if (digitalValue >= 720 && digitalValue <= 757) { // Blue span
      if (!conditionMet) {
        conditionStartTime = millis();
        conditionMet = true;
      } else if (millis() - conditionStartTime >= sensTime) {
        printf("temps Blue = ", millis() - conditionStartTime);
        // Condition met for at least 3 seconds
        digitalWrite(12, LOW);
        digitalWrite(11, HIGH); // Blue LED on
        digitalWrite(10, LOW);
        delay(onTime);
      }
    } else if (digitalValue >= 650 && digitalValue <= 710) { // Red span
      if (!conditionMet) {
        conditionStartTime = millis();
        conditionMet = true;
      } else if (millis() - conditionStartTime >= sensTime) {
        printf("temps Red = ", millis() - conditionStartTime);
        // Condition met for at least 3 seconds
        digitalWrite(12, HIGH); // Red LED on
        digitalWrite(11, LOW);
        digitalWrite(10, LOW);
        delay(onTime);
      }
    } else if (digitalValue >= 295 && digitalValue <= 420) { // White span
      if (!conditionMet) {
        conditionStartTime = millis();
        conditionMet = true;
      } else if (millis() - conditionStartTime >= sensTime) {
        printf("temps White = ", millis() - conditionStartTime);
        // Condition met for at least 3 seconds
        digitalWrite(12, LOW);
        digitalWrite(11, LOW);
        digitalWrite(10, HIGH); // White LED on
        delay(onTime);
      }
    } else { // Other colors
      conditionMet = false;
    }
  }
  digitalWrite(13, LOW);
  digitalWrite(12, LOW);
  digitalWrite(11, LOW);
  digitalWrite(10, LOW);
}