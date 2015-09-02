MAS n-queens problem
====================
Semestral project for Multi-Agent Systems (AE4M36MAS) course (OI FEE CTU).

Execution:
----------
- main class *StartAgents* taking single argument, the number of queens n 
- provided execution script *solve.sh <n>*
- script assumes an executable jar named *mas-nqueens.jar* in the root of the project

Dependencies:
-------------
- alite (ATG @ FEE CTU, snapshot located in *lib* folder)

Assignment:
-----------
Implement Java agents that will cooperatively solve the n-queen puzzle using the Asynchronous Backtracking (ABT) 
algorithm. There are N agents indexed 0,..,n-1 representing N queens on an NxN chessboard. Agent i controls 
the queen at the i-th row of the chessboard. The task is to find a corresponding column for each agent/queen such that 
no two agents/queens attack each other according to the classic rules of chess. The rows and columns are indexed as 
indicated on the picture at the left. The solution must be found in a decentralized manner using ABT 
(or possibly another complete asynchronous decentralized CSP algorithm). I.e., the agents must be able to detect that 
a) a valid solution has been found by all agents (if it exists) and b) that the NxN chessboard does not admit a valid 
solution.