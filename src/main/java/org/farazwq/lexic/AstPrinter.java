package org.farazwq.lexic;

class AstPrinter implements Expr.Visitor<String> {

  // Public method to start the printing process
  String print(Expr expr) {
    // Call accept on the expression, passing 'this' (the AstPrinter)
    // The expression's accept method will call the correct visit method below
    return expr.accept(this);
  }

  // Visit method for Binary expressions (e.g., 1 + 2)
  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    // Use the helper to create a parenthesized string
    // e.g., (+ 1 2)
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  // Visit method for Grouping expressions (e.g., (3))
  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    // Use the helper, naming the group "group"
    // e.g., (group 3)
    return parenthesize("group", expr.expression);
  }

  // Visit method for Literal expressions (e.g., 123, "hello", true)
  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    // Handle nil (represented by Java null)
    if (expr.value == null) return "nil";
    // Convert the value to a string
    return expr.value.toString();
  }

  // Visit method for Unary expressions (e.g., -5, !found)
  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    // Use the helper
    // e.g., (- 5)
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  // Helper method to format output with parentheses
  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    // Start the parenthesized group
    builder.append("(").append(name);

    // Loop through each sub-expression
    for (Expr expr : exprs) {
      // Add a space
      builder.append(" ");
      // Recursively call accept to print the sub-expression
      // This is how the tree traversal happens
      builder.append(expr.accept(this));
    }

    // Close the parentheses
    builder.append(")");

    // Return the final string
    return builder.toString();
  }

//   // Main method for quick testing (can be deleted later)
//   public static void main(String[] args) {
//     // Manually build the tree for: (- 123) * (group 45.67)
//     Expr expression = new Expr.Binary(
//         new Expr.Unary( // Left operand: (- 123)
//             new Token(TokenType.MINUS, "-", null, 1), // Token for '-'
//             new Expr.Literal(123) // Operand for unary minus
//         ),
//         new Token(TokenType.STAR, "*", null, 1), // Operator '*'
//         new Expr.Grouping( // Right operand: (group 45.67)
//             new Expr.Literal(45.67) // Expression inside the group
//         )
//     );

//     // Print the generated string representation of the tree
//     System.out.println(new AstPrinter().print(expression));
//     // Expected output: (* (- 123) (group 45.67))
//   }
}