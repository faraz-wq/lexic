# **Implementation of Recursive Descent Expression Parser**  

This commit introduces a **recursive descent parser** for Lexic expressions, handling arithmetic, comparisons, logical operators, and grouping with proper **precedence** and **associativity**.  

---

## **1. Grammar Rules & Parsing Strategy**  

The parser follows a **context-free grammar** (CFG) to structure expressions. Each grammar rule translates to a method in the parser:  

### **Grammar Rules (Simplified)**

| Rule | Definition | Purpose |
|------|-----------|---------|
| `expression` | `equality` | Top-level rule (lowest precedence). |
| `equality` | `comparison ( ( "!=" \| "==" ) comparison )*` | Handles `==` and `!=`. |
| `comparison` | `term ( ( ">" \| ">=" \| "<" \| "<=" ) term )*` | Handles `<`, `>`, etc. |
| `term` | `factor ( ( "+" \| "-" ) factor )*` | Handles addition/subtraction. |
| `factor` | `unary ( ( "*" \| "/" ) unary )*` | Handles multiplication/division. |
| `unary` | `( "!" \| "-" ) unary \| primary` | Handles `-x` and `!x`. |
| `primary` | `NUMBER \| STRING \| "true" \| "false" \| "nil" \| "(" expression ")"` | Base literals and parentheses. |

### **Note:**

- **Precedence** is enforced by the **order of method calls** (e.g., `term()` calls `factor()`, ensuring `*` binds tighter than `+`).  
- **Associativity** is controlled by **left-recursive loops** (e.g., `while (match(PLUS, MINUS))` makes `+` left-associative).  

---

## **2. Recursive Descent Parsing (How It Works)**  

A **recursive descent parser** is a top-down parsing technique where each **grammar rule** is translated directly into a **recursive function**. It’s called "recursive" because functions call themselves (or each other) to handle nested structures, and "descent" because parsing starts at the highest-level rule and works its way down to the terminals (tokens).

---

### **1. Core Principles**

#### **(1) Grammar-Driven Design**

- Each **production rule** in the grammar becomes a **function**.
- **Example Grammar Rule:**  

  ```md
  expression → equality ;
  equality → comparison ( ( "!=" | "==" ) comparison )* ;
  ```

  **→ Java Method:**  

  ```java
  private Expr expression() { return equality(); }
  private Expr equality() { /* ... */ }
  ```

#### **(2) Top-Down Parsing**

- Starts at the **highest-level rule** (`expression`) and drills down to **literals** (numbers, strings, etc.).
- **Example:** Parsing `1 + 2 * 3`  
  - `expression()` → `equality()` → `comparison()` → `term()` → `factor()` → `unary()` → `primary()` (finally reads `1`).

#### **(3) Precedence & Associativity**

- **Precedence** is enforced by **order of method calls** (higher precedence = deeper in the call stack).  
  - `term()` handles `+`/`-` but calls `factor()` first (so `*` binds tighter).  
- **Associativity** is controlled by **loops** (left-associative) or **recursion** (right-associative).  

---

### **2. How It Works Step-by-Step**

#### **Example: Parsing `1 + 2 * 3`**

1. **Start at `expression()`**  
   - Calls `equality()` → `comparison()` → `term()`.
2. **Inside `term()`:**  
   - Parses `1` (via `factor()` → `unary()` → `primary()`).
   - Sees `+`, so enters `while` loop:  

     ```java
     while (match(PLUS, MINUS)) {
         Token operator = previous(); // "+"
         Expr right = factor();      // Recursively parse `2 * 3`
         expr = new Binary(expr, operator, right);
     }
     ```

3. **Parsing `2 * 3` (in `factor()`):**  
   - Parses `2` (via `unary()` → `primary()`).
   - Sees `*`, enters loop:  

     ```java
     while (match(STAR, SLASH)) {
         Token operator = previous(); // "*"
         Expr right = unary();        // Parses `3`
         expr = new Binary(expr, operator, right);
     }
     ```

4. **Resulting AST:**  

   ```md
   Binary(
       left: Literal(1),
       operator: +,
       right: Binary(
           left: Literal(2),
           operator: *,
           right: Literal(3)
       )
   )
   ```

   (Mathematically correct: `1 + (2 * 3)`)

---

### **3. Handling Different Language Constructs**

#### **(1) Binary Operators (`+`, `*`, `==`, etc.)**

- **Structure:**  

  ```java
  private Expr term() {
      Expr expr = factor(); // Higher precedence first
      while (match(PLUS, MINUS)) {
          Token operator = previous();
          Expr right = factor(); // Parse right operand
          expr = new Binary(expr, operator, right);
      }
      return expr;
  }
  ```

  - **Key Insight:** The `while` loop ensures **left associativity** (e.g., `1 - 2 - 3` → `(1 - 2) - 3`).

#### **(2) Unary Operators (`!`, `-`)**

- **Structure:**  

  ```java
  private Expr unary() {
      if (match(BANG, MINUS)) {
          Token operator = previous();
          Expr right = unary(); // Recursive call (right-associative)
          return new Unary(operator, right);
      }
      return primary(); // Base case
  }
  ```

  - **Why Recursion?** For nested unaries like `!!x` → `!(!x)`.

#### **(3) Grouping (`( ... )`)**

- **Structure:**  

  ```java
  private Expr primary() {
      if (match(LEFT_PAREN)) {
          Expr expr = expression(); // Recurse into nested expression
          consume(RIGHT_PAREN, "Expect ')' after expression.");
          return new Grouping(expr);
      }
      // ... handle literals ...
  }
  ```

  - **Key Insight:** The parser **backtracks** to `expression()` after `(`.

---

### **4. Error Recovery (Panic Mode)**

To avoid cascading errors, the parser:

1. **Throws a `ParseError`** when encountering invalid syntax.
2. **Synchronizes** by discarding tokens until a known boundary (e.g., `;` or keyword).

   ```java
   private void synchronize() {
       while (!isAtEnd()) {
           if (previous().type == SEMICOLON) return;
           switch (peek().type) {
               case CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return;
           }
           advance();
       }
   }
   ```

## **3. Error Handling & Recovery**  

### **Goals:**

1. **Report errors clearly** (e.g., `Error: Expect ')' after expression`).  
2. **Avoid cascading errors** (stop misparsing after the first mistake).  
3. **Recover gracefully** (skip to the next statement).  

### **Key Methods:**

| Method | Purpose |
|--------|---------|
| `consume(TokenType, String)` | Ensures the next token matches (or throws an error). |
| `error(Token, String)` | Reports an error and returns a `ParseError` for unwinding. |
| `synchronize()` | Discards tokens until a safe point (e.g., `;` or keyword). |

### **Example: Parentheses Mismatch**

```java
if (match(LEFT_PAREN)) {
    Expr expr = expression();
    consume(RIGHT_PAREN, "Expect ')' after expression."); // Throws if missing
    return new Expr.Grouping(expr);
}
```

- If the user forgets `)`, `consume()` throws an error.  
- `synchronize()` prevents further misleading errors.  

---

## **4. Testing & Validation**  

The parser is tested by feeding tokens from:  

```java
Parser parser = new Parser(tokens);
Expr expr = parser.parse();
System.out.println(new AstPrinter().print(expr)); // Prints the syntax tree
```

### **Example Input/Output:**

| Input | Syntax Tree (AST) |
|-------|-------------------|
| `1 + 2 * 3` | `(+ 1 (* 2 3))` |
| `!true == false` | `(== (! true) false)` |
| `(1 + 2) * 3` | `(* (group (+ 1 2)) 3)` |

**Next Steps:**  

- Extend to statements (`if`, `while`, etc.).  
- Build the interpreter to evaluate parsed expressions.

***Recommended Reads***
- https://www.geeksforgeeks.org/theory-of-computation/what-is-context-free-grammar/
- https://en.wikipedia.org/wiki/Recursive_descent_parser
