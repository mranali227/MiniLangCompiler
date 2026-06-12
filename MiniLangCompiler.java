import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class MiniLangCompiler {

    static Set<String> declared = new HashSet<>();
    static List<String> intermediateCode = new ArrayList<>();
    static int labelCounter = 1;

    static String newLabel() {
        return "L" + (labelCounter++);
    }

    public static void main(String[] args) {
        try {
            String code = Files.readString(Path.of("input.min"));

            System.out.println("=== TOKENS ===");
            tokenize(code);

            System.out.println("\n=== COMPILATION SUCCESSFUL ===");
            compile(code);

            System.out.println("\n=== INTERMEDIATE CODE ===");
            for (String line : intermediateCode) {
                System.out.println(line);
            }

            Files.write(Path.of("output.txt"), intermediateCode);

            System.out.println("\nIntermediate code saved to output.txt");

        } catch (Exception e) {
            System.out.println("\nERROR: " + e.getMessage());
        }
    }

    // Lexical Analysis
    static void tokenize(String code) {
        Pattern pattern = Pattern.compile(
            "\\b(INT|IF|ELSE|PRINT|END)\\b" +
            "|\\b[a-zA-Z_][a-zA-Z0-9_]*\\b" +
            "|\\b\\d+\\b" +
            "|==|<=|>=|!=|[+\\-*/=<>;(){}]"
        );

        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            System.out.println(matcher.group());
        }
    }

    // Main Compilation Logic
    static void compile(String code) throws Exception {
        List<String> lines = Arrays.stream(code.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (!lines.get(lines.size() - 1).equals("END")) {
            throw new Exception("Program must end with END");
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.equals("END")) {
                break;
            }

            if (line.startsWith("INT ")) {
                handleDeclaration(line);
            } else if (line.startsWith("PRINT(")) {
                handlePrint(line);
            } else if (line.startsWith("IF")) {
                i = handleIf(lines, i);
            } else if (line.contains("=")) {
                handleAssignment(line);
            }
        }
    }

    // INT x = 5;
    static void handleDeclaration(String line) throws Exception {
        Pattern p = Pattern.compile("INT\\s+(\\w+)\\s*=\\s*(\\d+)\\s*;");
        Matcher m = p.matcher(line);

        if (!m.matches()) {
            throw new Exception("Syntax Error in declaration: " + line);
        }

        String var = m.group(1);
        String value = m.group(2);

        declared.add(var);
        intermediateCode.add(var + " = " + value);
    }

    // x = x + y; OR x = 10;
    static void handleAssignment(String line) throws Exception {
        Pattern arithmetic = Pattern.compile("(\\w+)\\s*=\\s*(\\w+)\\s*([+\\-])\\s*(\\w+)\\s*;");
        Matcher m1 = arithmetic.matcher(line);

        if (m1.matches()) {
            String dest = m1.group(1);
            String op1 = m1.group(2);
            String operator = m1.group(3);
            String op2 = m1.group(4);

            checkDeclared(dest);
            checkOperand(op1);
            checkOperand(op2);

            intermediateCode.add("t = " + op1 + " " + operator + " " + op2);
            intermediateCode.add(dest + " = t");
            return;
        }

        Pattern simple = Pattern.compile("(\\w+)\\s*=\\s*(\\d+)\\s*;");
        Matcher m2 = simple.matcher(line);

        if (m2.matches()) {
            String dest = m2.group(1);
            String value = m2.group(2);

            checkDeclared(dest);
            intermediateCode.add(dest + " = " + value);
            return;
        }

        throw new Exception("Syntax Error in assignment: " + line);
    }

    // PRINT(x);
    static void handlePrint(String line) throws Exception {
        Pattern p = Pattern.compile("PRINT\\((\\w+)\\)\\s*;");
        Matcher m = p.matcher(line);

        if (!m.matches()) {
            throw new Exception("Syntax Error in PRINT: " + line);
        }

        String var = m.group(1);
        checkDeclared(var);

        intermediateCode.add("PRINT " + var);
    }

    // IF (x < y) { ... } ELSE { ... }
    static int handleIf(List<String> lines, int index) throws Exception {
        Pattern p = Pattern.compile("IF\\s*\\((\\w+)\\s*([<>])\\s*(\\w+)\\)\\s*\\{");
        Matcher m = p.matcher(lines.get(index));

        if (!m.matches()) {
            throw new Exception("Syntax Error in IF statement: " + lines.get(index));
        }

        String left = m.group(1);
        String op = m.group(2);
        String right = m.group(3);

        checkOperand(left);
        checkOperand(right);

        String trueLabel = newLabel();
        String falseLabel = newLabel();
        String endLabel = newLabel();

        intermediateCode.add("IF " + left + " " + op + " " + right + " GOTO " + trueLabel);
        intermediateCode.add("GOTO " + falseLabel);
        intermediateCode.add(trueLabel + ":");

        // THEN block
        index++;
        while (index < lines.size() && !lines.get(index).equals("} ELSE {")) {
            if (lines.get(index).equals("}")) break;

            String line = lines.get(index);

            if (line.startsWith("PRINT(")) {
                handlePrint(line);
            } else if (line.contains("=")) {
                handleAssignment(line);
            }

            index++;
        }

        intermediateCode.add("GOTO " + endLabel);
        intermediateCode.add(falseLabel + ":");

        // ELSE block
        if (index < lines.size() && lines.get(index).equals("} ELSE {")) {
            index++;
            while (index < lines.size() && !lines.get(index).equals("}")) {
                String line = lines.get(index);

                if (line.startsWith("PRINT(")) {
                    handlePrint(line);
                } else if (line.contains("=")) {
                    handleAssignment(line);
                }

                index++;
            }
        }

        intermediateCode.add(endLabel + ":");
        return index;
    }

    static void checkDeclared(String var) throws Exception {
        if (!declared.contains(var)) {
            throw new Exception("Semantic Error: Variable '" + var + "' not declared");
        }
    }

    static void checkOperand(String operand) throws Exception {
        if (operand.matches("\\d+")) return;
        checkDeclared(operand);
    }
}
