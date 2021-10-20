package step1;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.Stack;

/**
 * Thompson算法
 * 编译器前端
 * 步骤1目的:将正则字符串转为NFA
 *
 * @author zhouang
 */
public class Thompson {
    private static final String USAGE = "输入一个正则表达式,本demo仅支持a-z的小写字母\n"
        + "运算符支持'*'、'a|b'、'ab'、'()'"
        + "退出请输入':q'或者'quit'";

    private static final char EMPTY = 'E';

    /**
     * 构造三元组 (from,字符,to)
     */
    public static class Trans {
        public int state_from, state_to;
        public char trans_symbol;

        public Trans(int v1, int v2, char sym) {
            this.state_from = v1;
            this.state_to = v2;
            this.trans_symbol = sym;
        }

        public Trans(int v1, int v2, int offset, char sym) {
            this.state_from = v1 + offset;
            this.state_to = v2 + offset;
            this.trans_symbol = sym;
        }
    }

    /**
     * NFA子图的表示
     */
    public static class NFA {
        public ArrayList<Integer> states;
        public ArrayList<Trans> transitions;
        public int final_state;

        public NFA() {
            this.states = new ArrayList<>();
            this.transitions = new ArrayList<>();
            this.final_state = 0;
        }

        public NFA(int size) {
            this.states = new ArrayList<>();
            this.transitions = new ArrayList<>();
            this.final_state = 0;
            this.initStateSize(size);
        }

        public NFA(char c) {
            this.states = new ArrayList<>();
            this.transitions = new ArrayList<>();
            this.initStateSize(2);
            this.final_state = 1;
            this.transitions.add(new Trans(0, 1, c));
        }

        public void initStateSize(int size) {
            for (int i = 0; i < size; i++) {this.states.add(i);}
        }

        public void display() {
            for (Trans t : transitions) {
                System.out.println("(" + t.state_from + ", " + t.trans_symbol +
                    ", " + t.state_to + ")");
            }
        }
    }

    /**
     * 克林闭包的转换 a*
     *
     * @param old
     * @return
     */
    public static NFA kleene(NFA old) {
        //新建立一个图
        NFA result = new NFA(old.states.size() + 2);

        //增加第一个边
        result.transitions.add(new Trans(0, 1, EMPTY));

        //复制原来的基础,整体编号都+1
        for (Trans t : old.transitions) {
            result.transitions.add(new Trans(t.state_from, t.state_to, 1, t.trans_symbol));
        }

        //增加尾部边
        result.transitions.add(new Trans(old.states.size(),
            old.states.size() + 1, 'E'));

        //添加回环边
        result.transitions.add(new Trans(old.states.size(), 1, EMPTY));

        //添加首尾边
        result.transitions.add(new Trans(0, old.states.size() + 1, EMPTY));

        //结束状态设为尾部节点
        result.final_state = old.states.size() + 1;
        return result;
    }

    /**
     * 拼接的转换 ab
     *
     * @param n 状态图n
     * @param m 状态图m
     * @return
     */
    public static NFA concat(NFA n, NFA m) {
        m.states.remove(0); // delete m's initial state

        //把m图的状态拼接在n图后
        for (Trans t : m.transitions) {
            n.transitions.add(new Trans(t.state_from, t.state_to, n.states.size() - 1, t.trans_symbol));
        }

        //添加m图的节点编号
        for (Integer s : m.states) {
            n.states.add(s + n.states.size() + 1);
        }

        n.final_state = n.states.size() + m.states.size() - 2;
        return n;

    }

    /**
     * 多选一的转换 a|b
     *
     * @param n 状态图n
     * @param m 状态图m
     * @return 合并后的状态图
     */
    public static NFA union(NFA n, NFA m) {
        //比原状态添加2个头尾新状态
        NFA result = new NFA(n.states.size() + m.states.size() + 2);

        //给上面的图增加首边
        result.transitions.add(new Trans(0, 1, EMPTY));

        //复制上边的老边
        for (Trans t : n.transitions) {
            result.transitions.add(new Trans(t.state_from, t.state_to, 1, t.trans_symbol));
        }

        //添加上面的尾边
        result.transitions.add(new Trans(n.states.size(),
            n.states.size() + m.states.size() + 1, EMPTY));

        //添加下面的首边
        result.transitions.add(new Trans(0, n.states.size() + 1, EMPTY));

        //复制下面的老边
        for (Trans t : m.transitions) {
            result.transitions.add(new Trans(t.state_from, t.state_to, n.states.size() + 1, t.trans_symbol));
        }

        //给下图增加尾边
        result.transitions.add(new Trans(m.states.size() + n.states.size(),
            n.states.size() + m.states.size() + 1, EMPTY));

        //设置终节点
        result.final_state = n.states.size() + m.states.size() + 1;
        return result;
    }

    /**
     * 验证是否为小写字母
     *
     * @param c
     * @return
     */
    private static boolean alpha(char c) {return c >= 'a' && c <= 'z';}

    public static boolean alphabet(char c) {return alpha(c) || c == EMPTY;}

    /**
     * 验证是否为操作符号
     *
     * @param c
     * @return
     */
    public static boolean regexOperator(char c) {
        return c == '(' || c == ')' || c == '*' || c == '|';
    }

    public static boolean validRegExChar(char c) {
        return alphabet(c) || regexOperator(c);
    }

    /**
     * 验证字符串是否满足要求
     *
     * @param regex
     * @return
     */
    public static boolean validRegEx(String regex) {
        if (regex.isEmpty()) {return false;}
        for (char c : regex.toCharArray()) {if (!validRegExChar(c)) {return false;}}
        return true;
    }

    /**
     * 正则表达式转为状态图
     *
     * @param regex
     * @return
     */
    public static NFA compile(String regex) {
        //只能含有字母和正则符号
        if (!validRegEx(regex)) {
            System.out.println("Invalid Regular Expression Input.");
            return new NFA(); // empty NFA if invalid regex
        }

        Stack<Character> operators = new Stack<>();
        Stack<NFA> operands = new Stack<>();
        Stack<NFA> concat_stack = new Stack<>();
        boolean ccflag = false; // concat flag
        char c; // current character of string
        //站内的'('数量
        int para_count = 0;

        //对基本单元进行合并
        //和算1+1一样,分为 数字栈和操作符栈
        for (int i = 0; i < regex.length(); i++) {
            c = regex.charAt(i);
            if (alphabet(c)) {
                //单个字母触发生成基本的图单元
                operands.push(new NFA(c));

                //是否连续字符
                if (ccflag) {
                    operators.push('.');
                } else {ccflag = true;}
            } else {
                if (c == ')') {
                    ccflag = false;
                    if (para_count == 0) {
                        System.out.println("Error: More end paranthesis " +
                            "than beginning paranthesis");
                        System.exit(1);
                    } else {para_count--;}

                    //不断弹出操作符,直到碰到'('
                    while (!operators.empty() && operators.peek() != '(') {
                        tryConcat(operators, operands, concat_stack);
                    }
                } else if (c == '*') {
                    operands.push(kleene(operands.pop()));
                    ccflag = true;
                } else if (c == '(') {
                    operators.push(c);
                    para_count++;
                } else if (c == '|') {
                    operators.push(c);
                    ccflag = false;
                }
            }
        }

        //遍历完的情况,主要是处理没有括号的情况,比如 aa|c
        while (operators.size() > 0) {
            if (operands.empty()) {
                System.out.println("Error: 操作数和操作符不匹配!");
                System.exit(1);
            }
            tryConcat(operators, operands, concat_stack);
        }
        return operands.pop();
    }

    private static void tryConcat(Stack<Character> operators, Stack<NFA> operands, Stack<NFA> concat_stack) {
        char op;
        NFA nfa2;
        NFA nfa1;
        op = operators.pop();
        if (op == '.') {
            nfa2 = operands.pop();
            nfa1 = operands.pop();
            operands.push(concat(nfa1, nfa2));
        } else if (op == '|') {
            nfa2 = operands.pop();
            if (!operators.empty() && operators.peek() == '.') {
                concat_stack.push(operands.pop());
                while (!operators.empty() && operators.peek() == '.') {
                    concat_stack.push(operands.pop());
                    operators.pop();
                }
                nfa1 = concat(concat_stack.pop(),
                    concat_stack.pop());
                while (concat_stack.size() > 0) {
                    nfa1 = concat(nfa1, concat_stack.pop());
                }
            } else {
                nfa1 = operands.pop();
            }
            operands.push(union(nfa1, nfa2));
        }
    }

    private static void showUsage() {
        System.out.println(USAGE);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String line;
        showUsage();
        while (sc.hasNextLine()) {
            showUsage();
            line = sc.nextLine();
            if (":q".equals(line) || "quit".equalsIgnoreCase(line)) {break;}
            NFA nfa_of_input = compile(line);
            System.out.println("\nNFA:");
            nfa_of_input.display();
        }
    }
}
