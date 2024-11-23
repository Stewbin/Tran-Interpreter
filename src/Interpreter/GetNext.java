package Interpreter;

import AST.BuiltInMethodDeclarationNode;
import Interpreter.DataTypes.BooleanIDT;
import Interpreter.DataTypes.InterpreterDataType;
import Interpreter.DataTypes.NumberIDT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class GetNext extends BuiltInMethodDeclarationNode {
    private final int[] nums;
    private int currPos;
    private int currVal;

    public GetNext(int n) {
        this.nums = IntStream.rangeClosed(0, n).toArray();
        currPos = 0;
        currVal = nums[0];
    }

    /**
     * Returns two things: a boolean, and the next element in iteration.
     * @return Value1: True if the iterator is not done, if else false. <br/>
     *         Value2: The next value in the collection. Returns -1 if no elements left.
     */
    @Override
    public List<InterpreterDataType> Execute(List<InterpreterDataType> params) {
        var retVals = new ArrayList<InterpreterDataType>(2);
        if (currPos < nums.length) {
            currVal = nums[currPos++];
            retVals.add(new BooleanIDT(true));
            retVals.add(new NumberIDT(currVal));
        } else {
            retVals.add(new BooleanIDT(false));
            retVals.add(new NumberIDT(-1));
        }
        return retVals;
    }
}
