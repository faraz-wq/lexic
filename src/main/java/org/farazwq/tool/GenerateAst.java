package org.farazwq.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
  public static void main(String[] args) throws IOException {
    // Check if the correct number of arguments (output directory) is provided
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64); // Exit code 64 often means command line usage error
    }
    String outputDir = args[0]; // Get the output directory from arguments

    // Define the AST for expressions and generate the code
    defineAst(outputDir, "Expr", Arrays.asList(
      "Binary   : Expr left, Token operator, Expr right",
      "Grouping : Expr expression",
      "Literal  : Object value",
      "Unary    : Token operator, Expr right"
    ));
  }

  private static void defineAst(
      String outputDir, String baseName, List<String> types)
      throws IOException {
    // Create the file path (e.g., /path/to/output/Expr.java)
    String path = outputDir + "/" + baseName + ".java";
    // Create a PrintWriter to write the Java code to the file
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    // Write the basic package and import statements
    writer.println("package org.farazwq.lexic;");
    writer.println();
    writer.println("import java.util.List;"); // Might be needed later
    writer.println();

    // Write the abstract base class declaration
    writer.println("abstract class " + baseName + " {");

    // Define the Visitor interface inside the base class
    defineVisitor(writer, baseName, types);

    // Loop through the provided type definitions and generate each subclass
    for (String type : types) {
      // Split the definition string (e.g., "Binary : Expr left, ...")
      String className = type.split(":")[0].trim(); // Get "Binary"
      String fields = type.split(":")[1].trim();    // Get "Expr left, ..."
      // Call defineType to generate the code for this specific subclass
      defineType(writer, baseName, className, fields);
    }

    // Add the abstract accept method for the Visitor pattern
    writer.println();
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");

    // Close the base class
    writer.println("}");

    // Close the file writer
    writer.close();
  }

  private static void defineVisitor(
      PrintWriter writer, String baseName, List<String> types) {
    // Write the start of the generic Visitor interface
    writer.println("  interface Visitor<R> {");

    // Loop through each defined type to create a visit method
    for (String type : types) {
      String typeName = type.split(":")[0].trim(); // e.g., "Binary"
      // Write the visit method signature for this type
      // e.g., R visitBinaryExpr(Binary expr);
      writer.println("    R visit" + typeName + baseName + "(" +
          typeName + " " + baseName.toLowerCase() + ");");
    }

    // Close the Visitor interface
    writer.println("  }");
  }

  private static void defineType(
      PrintWriter writer, String baseName,
      String className, String fieldList) {
    // Write the class declaration, nested inside the base class
    writer.println("  static class " + className + " extends " +
        baseName + " {");

    // --- Constructor Generation ---
    // Write the constructor signature based on the field list
    writer.println("    " + className + "(" + fieldList + ") {");

    // Split the field list into individual fields (e.g., "Expr left", "Token operator")
    String[] fields = fieldList.split(", ");
    // Loop to generate assignment statements in the constructor body
    for (String field : fields) {
      // Split the field definition to get the name (e.g., "left" from "Expr left")
      String name = field.split(" ")[1];
      // Write the assignment (e.g., this.left = left;)
      writer.println("      this." + name + " = " + name + ";");
    }
    // Close the constructor body
    writer.println("    }");

    // --- Field Generation ---
    writer.println(); // Add a blank line
    // Loop through the fields again to declare them as final instance variables
    for (String field : fields) {
      // Write the field declaration (e.g., final Expr left;)
      writer.println("    final " + field + ";");
    }

    // --- Visitor Pattern Accept Method ---
    writer.println(); // Add a blank line
    writer.println("    @Override"); // Indicate this overrides the base method
    // Write the accept method signature
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    // Write the call to the specific visit method on the visitor
    // e.g., return visitor.visitBinaryExpr(this);
    writer.println("      return visitor.visit" +
        className + baseName + "(this);");
    writer.println("    }");

    // Close the class definition
    writer.println("  }");
  }
}