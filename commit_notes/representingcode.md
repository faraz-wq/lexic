

# Representing Code

This commit implements the foundational concepts for representing lexic source code as an Abstract Syntax Tree (AST) in preparation for the parser and interpreter. It covers the theoretical underpinnings of context-free grammars and the practical application of the Visitor pattern in Java.

## Table of Contents

1.  [Why Represent Code as a Tree?](#why-represent-code-as-a-tree)
2.  [Context-Free Grammars (CFGs)](#context-free-grammars-cfgs)
3.  [Abstract Syntax Trees (ASTs)](#abstract-syntax-trees-asts)
4.  [Implementing the AST Structure](#implementing-the-ast-structure)
5.  [Working with Trees: The Visitor Pattern](#working-with-trees-the-visitor-pattern)
6.  [A Practical Example: The AST Printer](#a-practical-example-the-ast-printer)
7.  [Files Added/Modified](#files-addedmodified)

## Why Represent Code as a Tree?

When you evaluate an arithmetic expression like `1 + 2 * 3 - 4`, you understand the order of operations (multiplication happens before addition/subtraction). A tree is a natural way to represent this structure visually.

*   **Leaves:** Represent the operands (numbers like `1`, `2`, `3`, `4`).
*   **Branches/Interior Nodes:** Represent the operators (`+`, `-`, `*`).
*   **Root:** The operator evaluated last (in this case, the final `-`).

This structure makes it easy for a computer program (the interpreter) to evaluate the expression by working from the leaves up (post-order traversal).

**Example Tree for `1 + 2 * 3 - 4`**

```
        -
       / \
      +   4
     / \
    1   *
       / \
      2   3
```

1.  Evaluate `2 * 3` = `6`.
2.  Evaluate `1 + 6` = `7`.
3.  Evaluate `7 - 4` = `3`.

This hierarchical structure is a much more suitable representation for the interpreter than the raw string of characters.

## Context-Free Grammars (CFGs)

While tokens (from the scanner) are like the "letters" of our language, a Context-Free Grammar (CFG) defines the "grammar rules" for combining these tokens into valid "sentences" (expressions, statements).

*   **Why not Regular Expressions?** Regular expressions (used for lexical analysis) are good for flat sequences but struggle with nested structures (like parentheses within parentheses) that are common in programming languages.
*   **Power of CFGs:** CFGs can define these arbitrarily nested structures using recursive rules.
*   **Notation:** Rules are often written in **Backus-Naur Form (BNF)** or **Extended BNF (EBNF)**.
    *   Format: `rule_name -> stuff_it_can_be_made_of ;`
    *   Symbols:
        *   **Terminal:** A literal token (e.g., `123`, `"+"`, `if`).
        *   **Nonterminal:** A reference to another rule (e.g., `expression`, `statement`).
    *   **Recursion:** Rules can refer to themselves, allowing for nesting.

**Example lexic Expression Grammar (Simplified)**

```ebnf
expression     → literal
               | unary
               | binary
               | grouping ;

literal        → NUMBER | STRING | "true" | "false" | "nil" ;
grouping       → "(" expression ")" ;
unary          → ( "-" | "!" ) expression ;
binary         → expression operator expression ;
operator       → "==" | "!=" | "<" | "<=" | ">" | ">="
               | "+"  | "-"  | "*" | "/" ;
```

## Abstract Syntax Trees (ASTs)

Once we have a CFG, we can build a tree that matches the structure of a piece of code according to that grammar. An **Abstract Syntax Tree (AST)** is a simplified version of this parse tree, focusing only on the essential elements needed for interpretation (e.g., parentheses affect structure but aren't separate nodes).

Each node in the AST represents a construct in the code:
*   `Binary`: An operator with two operands (e.g., `1 + 2`).
*   `Unary`: An operator with one operand (e.g., `-5`).
*   `Literal`: A constant value (e.g., `123`, `"hello"`).
*   `Grouping`: An expression enclosed in parentheses (e.g., `(1 + 2)`).

## Implementing the AST Structure

Writing Java classes for each AST node type (`Binary`, `Unary`, etc.) manually is repetitive. This project uses **metaprogramming** – a script that generates these classes.

1.  **Base Class (`Expr.java`):** An abstract class `Expr` serves as the parent for all expression nodes.
2.  **Subclasses:** Each specific expression type is a nested static class within `Expr` (e.g., `Expr.Binary`).
3.  **Fields:** Each subclass has `final` fields to store its specific data (operands, operators, values).
4.  **Constructor:** Initializes the fields.
5.  **Metaprogramming Script (`tool/GenerateAst.java`):**
    *   Defines the structure of each node type (name and fields).
    *   Writes the Java code for the `Expr.java` file, including all subclasses, constructors, and fields.
    *   This script is *run once* to generate the code; the generated code is then compiled and used.

**Generated `Expr.java` Structure (Simplified)**

```java
abstract class Expr {
  // Visitor Interface (explained later)
  interface Visitor<R> { ... }

  // Nested subclass for Binary expressions
  static class Binary extends Expr {
    final Expr left;
    final Token operator;
    final Expr right;

    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    // Accept method for Visitor pattern (explained later)
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }
  }

  // Other nested subclasses (Unary, Literal, Grouping)...

  // Abstract accept method
  abstract <R> R accept(Visitor<R> visitor);
}
```

## Working with Trees: The Visitor Pattern

Once the AST is built, we need to perform operations on it (like evaluating it or printing it). Since nodes are of different types (`Binary`, `Literal`, etc.), we need a flexible way to operate on them.

*   **The Problem:** Writing `if (node instanceof Expr.Binary) { ... } else if ...` is inefficient and messy.
*   **The Solution:** The **Visitor Pattern**.
*   **How it Works:**
    1.  Define a `Visitor` interface (nested in `Expr`) with a `visitX` method for each concrete node type (e.g., `visitBinaryExpr`).
    2.  Add an `abstract <R> R accept(Visitor<R> visitor)` method to the base `Expr` class.
    3.  Each concrete node class (`Binary`, `Literal`, etc.) implements `accept` by calling the corresponding `visitor.visitX(this)` method on the visitor object passed to it.
    4.  To perform an operation (like printing), create a class that implements `Expr.Visitor<DesiredReturnType>`. Implement the `visitX` methods with the specific logic for that operation on that node type.
    5.  Call `rootNode.accept(myVisitorInstance)` on the AST. Java's polymorphism ensures the correct `visitX` method in your visitor class is called for each node type encountered during the traversal.

This pattern decouples the operations (like evaluation or printing) from the node structure, making it easy to add new operations without modifying the node classes.

## A Practical Example: The AST Printer

The `AstPrinter.java` class demonstrates the Visitor pattern. It implements `Expr.Visitor<String>` to convert an AST back into a textual representation, showing its nested structure (similar to Lisp syntax).

**Example:**

*   **Code:** `(-123) * (group 45.67)`
*   **AST Printer Output:** `(* (- 123) (group 45.67))`

This output clearly shows the operator precedence and grouping, which is invaluable for debugging the parser.

## Files Added/Modified

*   `tool/GenerateAst.java`: The metaprogramming script used to generate the `Expr.java` file.
*   `lexic/Expr.java`: **(Generated File)** The core AST node classes (`Binary`, `Unary`, `Literal`, `Grouping`) and the `Visitor` interface. This file is created by running `GenerateAst.java`.
*   `lexic/AstPrinter.java`: An implementation of the `Expr.Visitor<String>` interface that prints the structure of an AST in a Lisp-like format for debugging.

---