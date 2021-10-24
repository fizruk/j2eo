package tree.Type;

import lexer.Scanner;
import tree.CompoundName;
import tree.Entity;

// UnannotatedType
//       : ...
//       | CompoundName
//       | CompoundName TypeArguments
//       | ...
//       ;
// TypeArguments
//       : LESS TypeArgumentList GREATER
//       ;
public class TypeName extends UnannotatedType
{
    // Structure
    public CompoundName compoundName;
    public TypeArguments typeArguments;

    // Creation
    public TypeName(CompoundName cn, TypeArguments targs)
    {
        super(null);
        this.compoundName = cn;
        this.typeArguments = targs;

        System.out.println("Type name accepted");
    }

    // Reporting
    public void report(int sh)
    {
        Entity.doShift(sh);
        System.out.print("TYPE ");
        compoundName.report(0);
        System.out.println();
        if ( super.annotations != null )
            super.annotations.report(sh+Entity.shift);

        if ( typeArguments != null )
            typeArguments.report(sh+ Entity.shift);

        if ( super.dimensions != null && super.dimensions.dimensions.size() > 0 )
            super.dimensions.report(sh+ Entity.shift);
    }

}