/**
 * Boolean - boolean/Boolean.
 * Numeric - byte/Byte, short/SHort, int/Integer, long/Long.
 * Decimal - float/Float, double/Double.
 * Character - char/Character.
 * String - String.
 *
 * <p>Syntax: [access_specifier] [access_modifier] [type] [name] = ...;</p>
 *
 * <p>Primitive types - boolean, byte, short, int, long, float, double, char Reference types -
 * Boolean, Byte, Short, Integer, Long, Float, Double, Character, String and all class ojects
 */
public class Variables {

  public int sumOfTwoNumbers(int value1, int value2) {
    return value1 + value2;
  }

  public void changeValueOfInput(Integer input) {
    input = 10;
  }

  /**
   * Main method.
   *
   * @param args Not used.
   */
  public static void main(String[] args) {
    // Declaration.
    int number1;
    // Initialization.
    number1 = 100_000;
    int number2 = 2_000_000;

    Variables object = new Variables();
    int sum = object.sumOfTwoNumbers(number1, number2);
    System.out.println(sum);

    int value1 = 100;
    Integer value2 = 200;

    System.out.println("value1 = " + value1);
    System.out.println("value2 = " + value2);

    final Integer value1Copy = value1;
    final Integer value2Copy = value2;

    object.changeValueOfInput(value1);
    object.changeValueOfInput(value2);

    System.out.println("After calling changeValueOfInput() for both values.");
    System.out.println("value1 = " + value1);
    System.out.println("value2 = " + value2);

    value1 = 11;
    value2 = 12;

    System.out.println("After changing both values.");
    System.out.println("value1 = " + value1);
    System.out.println("value2 = " + value2);
    System.out.println("value1Copy = " + value1Copy);
    System.out.println("value2Copy = " + value2Copy);

    // Pass by reference - Special case of pass by value where the value passed is the address.
    StringBuilder strValue1 = new StringBuilder("LOL");
    StringBuilder strValue1Copy = strValue1;
    System.out.println("strValue1 = " + strValue1.toString());
    System.out.println("strValue1Copy = " + strValue1Copy.toString());

    strValue1.append("L");

    System.out.println("strValue1 = " + strValue1.toString());
    System.out.println("strValue1Copy = " + strValue1Copy.toString());
  }
}
