/*****************************************************************
 *
 * PDASimulator
 * Garrett Goshorn
 * *
 * This program acts as a Push-down Automata simulator. It creates the "PDA" through a separate .txt file
 * named "PDA.txt". It then takes a string from the user as input and runs it through the PDA to determine
 * if it is an accepted string or not. To end the program, type "quit".
 * *
 *****************************************************************/


import java.io.*;
import java.util.*;

public class PDASimulator {

  static class Transition {
    int currentState;
    String inputChar;
    String popStack;
    int nextState;
    String pushStack;

    /****************************************************************
     *Transition
     **
     Purpose: This constructor allows transitions to be created that have unique information
     ====================================================================*/

    public Transition(int currentState, String inputChar, String popStack, int nextState, String pushStack) {
      this.currentState = currentState;
      this.inputChar = inputChar;
      this.popStack = popStack;
      this.nextState = nextState;
      this.pushStack = pushStack;
    }
  }

  private int numStates;
  private Set<Integer> acceptStates;
  private Set<String> inputAlphabet;
  private Set<String> stackAlphabet;
  private String bottomOfStackSymbol;
  private Map<String, Transition> transitions;
  private int deadState;

  public PDASimulator() {
    acceptStates = new HashSet<>();
    inputAlphabet = new HashSet<>();
    stackAlphabet = new HashSet<>();
    transitions = new HashMap<>();
  }

  /****************************************************************
   *loadPDA(String filename)
   **
   Purpose: This function takes a string as input, which is just the inputted PDA file (PDA.txt). It
   * then reads the given file and stores the number of states, the accepting state, the alphabets, and
   * the transitions into their own global variables. This function is what essentially creates the PDA.
   ====================================================================*/
  public void loadPDA(String filename) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    numStates = Integer.parseInt(br.readLine().trim()); //Number of states
    deadState = numStates;
    String[] acceptStatesTokens = br.readLine().trim().split("\\s+"); //Accepting states
    for (String token : acceptStatesTokens) {
      acceptStates.add(Integer.parseInt(token));
    }

    String[] inputAlphabetTokens = br.readLine().trim().split("\\s+"); //Input alphabet
    inputAlphabet.addAll(Arrays.asList(inputAlphabetTokens));

    String[] stackAlphabetTokens = br.readLine().trim().split("\\s+"); //Stack alphabet
    stackAlphabet.addAll(Arrays.asList(stackAlphabetTokens));
    bottomOfStackSymbol = stackAlphabetTokens[stackAlphabetTokens.length - 1];

    String line;
    while ((line = br.readLine()) != null) { //Transitions
      line = line.trim();
      if (line.isEmpty()) continue;
      String[] parts = line.split("->");
      String lhs = parts[0].trim();
      String rhs = parts[1].trim();
      lhs = lhs.replaceAll("\\s+", "");

      String[] lhsParts = lhs.split("");
      int currentState = Integer.parseInt(lhsParts[0]);
      String inputChar = lhsParts[1];
      String popStack = lhsParts[3];
      if(inputChar.equals("{") && lhsParts[2].equals("e") && lhsParts[3].equals("}")) {
        inputChar = "{e}";
        popStack = lhsParts[5];
      }

      String[] rhsParts = rhs.split("/");
      int nextState = Integer.parseInt(rhsParts[0]);
      String pushStack = rhsParts[1];

      Transition transition = new Transition(currentState, inputChar, popStack, nextState, pushStack);
      String key = currentState + "," + inputChar + "," + popStack;
      transitions.put(key, transition);
    }
    br.close();
  }

  /****************************************************************
   *simulate()
   **
   Purpose: This function requests input from the user for an input string. It checks if the user
   entered "quit" to determine if it should exit. If not, it then checks the input to see if it is valid
   with isValidInput(inputString). If that passes, then it computes the input with compute(inputString).
   If that function returns true, the string is accepted. If it returns false, the string is rejected.
   ====================================================================*/
  public void simulate() {
    Scanner scanner = new Scanner(System.in);
    System.out.println(">>>Loading PDA.txt…");
    while (true) {
      System.out.print(">>>Please enter a string to evaluate: \n");
      String inputString = scanner.nextLine();
      if (inputString.equalsIgnoreCase("Quit")) {
        System.out.println(">>>Goodbye!");
        break;
      }
      if (!isValidInput(inputString)) {
        System.out.println("INVALID INPUT");
        continue;
      }
      System.out.println(">>>Computation…");
      boolean accepted = compute(inputString);
      if (accepted) {
        System.out.println("ACCEPTED");
      } else {
        System.out.println("REJECTED");
      }
    }
    scanner.close();
  }

  /****************************************************************
   *isValidInput(String input)
   **
   Purpose: This function takes a string as input to determine if it is valid. It does this by iterating
   through the entire input string to look for any characters that aren't in the input alphabet. If it
   finds any character that isn't in the input alphabet, it returns false, otherwise true.
   ====================================================================*/
  private boolean isValidInput(String input) {
    for (char c : input.toCharArray()) {
      if (!inputAlphabet.contains(String.valueOf(c))) {
        return false;
      }
    }
    return true;
  }

  /****************************************************************
   *compute(String inputString)
   **
   Purpose: This function does the workload of determining if a given string is accepted or rejected by
   the created PDA. It utilizes a double-ended queue as the PDA stack. It iterates through each character
   in the input string and performs the appropriate computations to determine the transitions.
   ====================================================================*/
  private boolean compute(String inputString) {
    Deque<String> stack = new ArrayDeque<>();
    stack.push(bottomOfStackSymbol); //Pushes stack bottom symbol first
    int currentState = 0;
    String remainingInput = inputString;

    while (true) {
      String beforeStack = stack.isEmpty() ? "{e}" : stack.peek();
      String beforeInput = remainingInput;
      if(remainingInput.isEmpty())
        beforeInput = "{e}";

      String stackTop = stack.isEmpty() ? "" : stack.peek();
      String inputChar = remainingInput.isEmpty() ? "{e}" : String.valueOf(remainingInput.charAt(0));
      String key = currentState + "," + inputChar + "," + stackTop;

      Transition transition = transitions.get(key);

      if (transition == null && !remainingInput.isEmpty()) {
        key = currentState + "," + "{e}" + "," + stackTop;
        transition = transitions.get(key);
      }

      if (transition == null) {
        System.out.println(currentState + ", " + (beforeInput.isEmpty() ? "{e}" : beforeInput) + "/" + (beforeStack.isEmpty() ? "{e}" : beforeStack));
        break;
      }

      if (!stack.isEmpty()) {
        stack.pop();
      }

      String nextRemainingInput = remainingInput;
      if (!remainingInput.isEmpty() && transition.inputChar.equals(inputChar)) {
        nextRemainingInput = remainingInput.substring(1);
      }

      if (!transition.pushStack.equals("{e}")) {
        String symbolsToPush = new StringBuilder(transition.pushStack).reverse().toString();
        for (char c : symbolsToPush.toCharArray()) {
          stack.push(String.valueOf(c));
        }
      }

      String afterStack = getStackContent(stack);
      int nextState = transition.nextState;

      System.out.println(currentState + ", " + (beforeInput.isEmpty() ? "{e}" : beforeInput) + "/" + beforeStack + " -> " +
              nextState + ", " + (nextRemainingInput.isEmpty() ? "{e}" : nextRemainingInput) + "/" + (afterStack.isEmpty() ? "{e}" : afterStack));

      currentState = nextState;
      remainingInput = nextRemainingInput;

      if (remainingInput.isEmpty() && acceptStates.contains(currentState)) {
        System.out.println(currentState + ", {e}/" + (stack.isEmpty() ? "{e}" : stack.peek()));
        return true;
      }

      if (currentState == deadState) {
        return false;
      }
    }
      if(acceptStates.contains(currentState))
        System.out.println(currentState + ", {e}/" + (stack.isEmpty() ? "{e}" : stack.peek()));
    return acceptStates.contains(currentState);
  }

  /****************************************************************
   *getStackContent(Deque<String> stack)
   **
   Purpose: This function takes a double-ended queue as input and returns the stack contents as a string
   ====================================================================*/
  private String getStackContent(Deque<String> stack) {
    StringBuilder sb = new StringBuilder();
    Iterator<String> iterator = stack.descendingIterator();
    while (iterator.hasNext()) {
      sb.append(iterator.next());
    }
    return sb.reverse().toString();
  }

  public static void main(String[] args) {
    PDASimulator simulator = new PDASimulator();
    try {
      simulator.loadPDA("src/PDA.txt");
      simulator.simulate();
    } catch (IOException e) {
      System.err.println("Error loading PDA.txt: " + e.getMessage());
    }
  }
}
