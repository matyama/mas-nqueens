package mas.agent;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import mas.agent.MASQueenAgent;
import mas.agent.MASQueenAgent.OnAgentFinishedCallback;
import mas.agent.student.MyQueenAgent;
import cz.agents.alite.communication.DefaultCommunicator;
import cz.agents.alite.communication.channel.CommunicationChannelException;
import cz.agents.alite.communication.channel.DirectCommunicationChannel;
import cz.agents.alite.communication.channel.DirectCommunicationChannel.ReceiverTable;

public class StartAgents {

    public static void main(String[] args) {

		if (args.length != 1) {
			System.out.println("Invalid number of arguments. Expected 1, given: " + args.length);
			System.exit(1);
		}

        startAgents(Integer.valueOf(args[0]));
    }

    public static void startAgents(int nAgents) {
        ReceiverTable receiverTable = new DirectCommunicationChannel.DefaultReceiverTable();
        LinkedList<String> agentNames = new LinkedList<String>();
        List<MASQueenAgent> agents = new LinkedList<MASQueenAgent>(); 
        
        final Set<Integer> runningAgents = Collections.synchronizedSet(new HashSet<Integer>());
        final int[] solution = new int[nAgents];

        for (int i = 0; i < nAgents; i++) {
            agentNames.add(Integer.toString(i));
        }

        for (int i = 0; i < nAgents; i++) {
            System.out.println(">>> Adding agent " + i);

            final MASQueenAgent agent = new MyQueenAgent(i, nAgents);

            DefaultCommunicator communicator = new DefaultCommunicator(Integer.toString(i));
            try {
                communicator.addChannel(new DirectCommunicationChannel(communicator, receiverTable));
            } catch (CommunicationChannelException e) {
                e.printStackTrace();
            }
            communicator.addMessageHandler(agent);

            // setup communication infrastructure
            agent.setCommunicator(communicator, agentNames);
            
            agents.add(agent);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }
        
        for (final MASQueenAgent agent : agents) {
        	runningAgents.add(agent.getAgentId());
        	agent.registerAgentFinishedCallback(new OnAgentFinishedCallback() {
				
				@Override
				public void agentFinishedSuccess(int agentId, int finalColumn) {
					System.out.println(">>> Agent " + agentId + " reports that the search was terminated");
					runningAgents.remove(agentId);
					solution[agentId] = finalColumn;
					
					if (runningAgents.isEmpty()) {
						System.out.println(">>> All agents reported that the search terminated successfully. The solution is:\n");
						printoutSolution(solution);
					}
				}
				
				@Override
				public void agentFinishedFailure() {
					System.out.println(">>> One of the agents reported that no solution exists.");
				}
			});
        	
        	new Thread( new Runnable() {
				
				@Override
				public void run() {
					agent.run();
				}
			}).start();
        }
    }
    
    /**
     * Prints out an n-queens problem solution as a text grid.
     * @param solution array, where agentId-th element represents the index of column occupied by queen at (agentId+1)-th row.
     */
	private static void printoutSolution(int[] solution) {
		for (int i=0; i < solution.length; i++) {
			System.out.print("|");
			for (int j=0; j < solution.length; j++) 
				if (solution[i] == j) {
					System.out.print("Q|");
				} else {
					System.out.print(" |");
				}
			System.out.println();
		}
	}
}

