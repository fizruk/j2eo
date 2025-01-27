package tree.Statement;

import java.util.ArrayList;
import lexer.Token;
import tree.Entity;

// Statement
//    : SimpleStatement
//    | LabeledStatement
//    | IfThenElseStatement
//    | WhileStatement
//    | ForStatement
//    ;
//
// SimpleStatement
//    : Block
//    | SEMICOLON                                    // EmptyStatement
//    | StatementExpression SEMICOLON                // ExpressionStatement
//
//    | ASSERT Expression                  SEMICOLON // AssertStatement
//    | ASSERT Expression COLON Expression SEMICOLON // AssertStatement
//
//    | SWITCH LPAREN Expression RPAREN SwitchBlock  // SwitchStatement
//    | DO Statement WHILE LPAREN Expression RPAREN SEMICOLON // DoStatement
//
//    | BREAK            SEMICOLON             // BreakStatement
//    | BREAK IDENTIFIER SEMICOLON             // BreakStatement
//
//    | CONTINUE             SEMICOLON         // ContinueStatement
//    | CONTINUE IDENTIFIER  SEMICOLON         // ContinueStatement
//
//    | RETURN             SEMICOLON           // ReturnStatement
//    | RETURN Expression  SEMICOLON           // ReturnStatement
//
//    | SYNCHRONIZED LPAREN Expression RPAREN Block  // SynchronizedStatement
//
//    | THROW Expression SEMICOLON      // ThrowStatement
//    | YIELD Expression SEMICOLON      // YieldStatement
//
//      // TryStatement
//    | TRY Block Catches
//    | TRY Block Catches Finally
//    | TRY Block         Finally
//    | TRY ResourceSpecification Block CatchesOpt FinallyOpt // TryWithResourcesStatement
//    ;
//
// StatementExpression
//    : Assignment
//    | PreIncrementExpression
//    | PreDecrementExpression
//    | PostIncrementExpression
//    | PostDecrementExpression
//    | MethodInvocation
//    | ClassInstanceCreationExpression
//    ;
public class Statement extends Entity {
    // Structure
    public ArrayList<String> labels;

    // Creation
    public Statement(ArrayList<String> ls) {
        this.labels = ls;
    }

    public Statement addLabel(Token label) {
        if (labels == null) {
            labels = new ArrayList<>();
        }
        labels.add(label.image);
        return this;
    }

    // Reporting
    public void report(int sh) {
        if (labels == null || labels.size() == 0) {
            return;
        }

        StringBuilder labs = new StringBuilder("LABELS: ");
        for (String lab : labels) {
            labs.append(lab).append(" ");
        }
        Entity.doShift(sh);
        System.out.println(labs);
    }
}
