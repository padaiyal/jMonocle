/**
 * Methods and functions are names used for the same thing most of the time. But in older languages,
 * methods are used to refer to functions that are within classes.
 *
 * <p>Syntax: [access_specifier] [access_modifier] [return_type] [function_name]([parameters]) { ...
 * }
 *
 * <p>[return_type] and [function_name] are mandatory. The remaining are optional.
 */
public class Methods {

  /**
   * Main method of this class.
   *
   * @param args Input arguments.
   */
  public static void main(String[] args) {
    System.out.println("Bonjour!");

    Methods object = new Methods();
    int sum = object.sumOfTwoNumbers(5, 7);
    System.out.println(sum);
  }

  public int sumOfTwoNumbers(int value1, int value2) {
    return value1 + value2;
  }
}
