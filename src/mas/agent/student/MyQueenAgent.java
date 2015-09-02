package mas.agent.student;

import java.util.*;

import cz.agents.alite.communication.Message;
import cz.agents.alite.communication.content.Content;
import mas.agent.MASQueenAgent;

/**
 * @author matyama2
 */
public class MyQueenAgent extends MASQueenAgent {

    private static final int NONE = -1;

    private static final String SOLUTION = "*";
    private static final String NO_SOLUTION = "";

    private static final String OK = "Ok?";
    private static final String NO_GOOD = "NoGood";
    private static final String ADD_LINK = "AddLink";
    private static final String STOP = "Stop";

    private static final String OK_MSG = "Ok?(%d)"; // Ok?(sender.agentId <- arg)
    private static final String NO_GOOD_MSG = "NoGood(%s)"; // NoGood(sender -> receiver, [nogood])
    private static final String ADD_LINK_MSG = "AddLink"; // AddLink(receiver -> sender)
    private static final String STOP_MSG = "Stop(%s)"; // Stop(solution)

    private int[] priorities; // priority ordering [nAgents-1,...,0]

    private int[] domain; // domain[agentId] = agentId, i.e. {0,...,nAgents-1}

    private int value; // currently assigned value

    private int cost;

    private Set<Integer> tag;

    private boolean exact;

    private Map<Integer, Integer> localView;

    private Map<Integer, Set<NoGood>> nogoods;

    private Set<Integer> lowerAgents;

    public MyQueenAgent(int agentId, int nAgents) {
    	super(agentId, nAgents);
	}

	@Override
	protected void start(int agentId, int nAgents) {

        priorities = new int[nAgents];
        domain = new int[nAgents];
        tag = new HashSet<>(nAgents-1);
        localView = new HashMap<>();
        nogoods = new HashMap<>();
        lowerAgents = new HashSet<>(nAgents-1);

        for (int i = 0; i < nAgents; i++) {
            priorities[i] = nAgents-1 - i;
            domain[i] = i;
            nogoods.put(i, new HashSet<NoGood>());
            if (agentId < i) {
                lowerAgents.add(i);
            }
        }

        value = domain[0];

        for (int recId : lowerAgents) {
            sendOk(recId, value);
        }

	}

	@Override
	protected void processMessages(List<Message> newMessages) {
        for (Message message : newMessages) {
            handleMessage(message.getSender(), message.getContent().toString());
        }
        // try { Thread.sleep(1000); } catch (InterruptedException e) {e.printStackTrace();} // slow down
	}

    private String readHead(String msg) {
        return msg.indexOf('(') == NONE ? msg : msg.substring(0, msg.indexOf('('));
    }

    private int readVal(String msg) {
        return Integer.valueOf(msg.substring(msg.indexOf('(') + 1, msg.indexOf(')')));
    }

    private String readNoGood(String msg) {
        return msg.substring(msg.indexOf('(') + 1, msg.indexOf(')'));
    }
    private String readSolution(String msg) {
        return msg.substring(msg.indexOf('(') + 1, msg.indexOf(')'));
    }

    private String writeNoGood(int recId) {
        StringBuilder nogood = new StringBuilder(); // value:context:tag:cost:exact
        nogood.append(localView.get(recId)).append(':');
        int i = 0;
        for (int otherAgentId : localView.keySet()) {
            if (otherAgentId != recId) {
                if (i++ > 0) {
                    nogood.append('&');
                }
                nogood.append(otherAgentId).append('=').append(localView.get(otherAgentId));
            }
        }
        nogood.append(':');
        i = 0;
        for (int tagId : tag) {
            if (i++ > 0) {
                nogood.append('&');
            }
            nogood.append(tagId);
        }
        nogood.append(':').append(cost).append(':').append(exact);
        return nogood.toString();
    }

    private void handleMessage(String sender, String msg) {
        switch (readHead(msg)) {
            case OK: handleOk(Integer.valueOf(sender), readVal(msg));
                break;
            case NO_GOOD: handleNoGood(readNoGood(msg));
                break;
            case ADD_LINK: handleAddLink(Integer.valueOf(sender));
                break;
            case STOP: handleStop(readSolution(msg));
                break;
            default: throw new IllegalArgumentException("Unknown message head for: " + msg);
        }
    }

    private void sendOk(int recId, int myVal) {
        sendMessage(Integer.toString(recId), new MessageContent(String.format(OK_MSG, myVal)));
    }

    private void handleOk(int senderId, int senderVal) {

        // check nogood store
        Map<Integer, List<NoGood>> obsolete = new HashMap<>();
        for (int bannedVal : nogoods.keySet()) {
            for (NoGood nogood : nogoods.get(bannedVal)) {
                for (Integer ngAgentId : nogood.conds.keySet()) {
                    if (ngAgentId == senderId) {
                        if (nogood.conds.get(ngAgentId) != senderVal) {
                            if (!obsolete.containsKey(bannedVal)) {
                                obsolete.put(bannedVal, new LinkedList<NoGood>());
                            }
                            obsolete.get(bannedVal).add(nogood);
                        }
                        break;
                    }
                }
            }
        }

        for (int val : obsolete.keySet()) {
            nogoods.get(val).removeAll(obsolete.get(val));
        }

        localView.put(senderId, senderVal);
        adjustValue();
    }

    private void sendAddLink(int recId) {
        sendMessage(Integer.toString(recId), new MessageContent(ADD_LINK_MSG));
    }

    private void handleAddLink(int senderId) {
        lowerAgents.add(senderId);
        sendOk(senderId, value);
    }

    private void sendNoGood(int recId, String nogood) {
        sendMessage(Integer.toString(recId), new MessageContent(String.format(NO_GOOD_MSG, nogood)));
    }

    private void handleNoGood(String msgBody) {

        NoGood recNogood = new NoGood(msgBody);

        for (int otherAgentId : recNogood.conds.keySet()) {
            if (!localView.containsKey(otherAgentId)) {
                localView.put(otherAgentId, recNogood.conds.get(otherAgentId));
                sendAddLink(otherAgentId);
            } else if (!localView.get(otherAgentId).equals(recNogood.conds.get(otherAgentId))) {
                return;
            }
        }

        Map<Integer, List<NoGood>> obsolete = new HashMap<>();
        for (NoGood nogood : nogoods.get(recNogood.v)) {
            if (recNogood.tag.containsAll(nogood.tag)) {
                if (!obsolete.containsKey(recNogood.v))
                    obsolete.put(recNogood.v, new LinkedList<NoGood>());
                obsolete.get(recNogood.v).add(nogood);
            }
        }

        for (int v : obsolete.keySet()) {
            nogoods.get(v).removeAll(obsolete.get(v));
        }

        nogoods.get(recNogood.v).add(recNogood);

        adjustValue();

    }

    private int selectLowestPriorityAgent() {
        int minPriority = Integer.MAX_VALUE;
        int min = NONE;
        for (int otherAgentId : localView.keySet()) {
            if (priorities[otherAgentId] < minPriority) {
                minPriority = priorities[otherAgentId];
                min = otherAgentId;
            }
        }
        return min;
    }

    private void adjustValue() {

        int oldVal = value;
        cost = Integer.MAX_VALUE;

        for (int v : domain) {

            int delta = 0, lb = 0;
            boolean exact = true;

            Set<Integer> tag = new HashSet<>(nAgents());
            tag.add(getAgentId());

            for (int otherAgentId : localView.keySet()) {
                if (!consistent(getAgentId(), v, otherAgentId, localView.get(otherAgentId))) delta++;
            }

            for (NoGood nogood : nogoods.get(v)) {
                lb += nogood.cost;
                tag.addAll(nogood.tag);
                exact &= nogood.exact;
            }
            exact &= tag.containsAll(lowerAgents);

            if (delta + lb <= cost) {
                value = v;
                cost = delta + lb;
                this.tag = tag;
                this.exact = exact;
            }

        }

        if (cost != 0 || exact) {
            if (priorities[getAgentId()] == nAgents()-1 && exact) {
                sendStop(cost);
                return;
            }
            int rectId = selectLowestPriorityAgent();
            if (rectId != NONE) {
                sendNoGood(rectId, writeNoGood(rectId));
            }
        }

        if (value != oldVal) {
            for (int otherAgentId : lowerAgents) {
                sendOk(otherAgentId, value);
            }
        }

    }

    private void sendStop(int cost) {
        if (cost > 0) {
            broadcast(new MessageContent(String.format(STOP_MSG, NO_SOLUTION)));
            notifySolutionDoesNotExist();
        } else {
            broadcast(new MessageContent(String.format(STOP_MSG, SOLUTION)));
            notifySolutionFound(value);
        }
    }

    private void handleStop(String solution) {
        if (SOLUTION.equals(solution)) {
            notifySolutionFound(value);
        } else {
            notifySolutionDoesNotExist();
        }
    }

    private boolean consistent(int i, int xi, int j, int xj) {
        return xi != xj && Math.abs(xi - xj) != Math.abs(i - j);
    }

    @SuppressWarnings("serial")
    private static class MessageContent extends Content {

        public final String content;

        public MessageContent(String content) {
            super(content);
            this.content = content;
        }

        @Override
        public String toString() {
            return content;
        }

    }

    private class NoGood {

        public final int v;
        public final Map<Integer, Integer> conds;
        public final int cost;
        public final Set<Integer> tag;
        public final boolean exact;

        private final int hash;

        public NoGood(String nogood) {

            String[] parts = nogood.split(":"); // v:[j=xj&k=xk]:[a&b]:cost:exact

            v = Integer.valueOf(parts[0]);

            StringTokenizer st = new StringTokenizer(parts[1], "&");
            conds = new HashMap<>();
            while (st.hasMoreTokens()) {
                String pair = st.nextToken();
                int idx = pair.indexOf("=");
                conds.put(Integer.valueOf(pair.substring(0, idx)), Integer.valueOf(pair.substring(idx+1)));
            }

            st = new StringTokenizer(parts[2], "&");
            tag = new HashSet<>(st.countTokens());
            while (st.hasMoreTokens()) {
                tag.add(Integer.valueOf(st.nextToken()));
            }

            cost = Integer.valueOf(parts[3]);
            exact = Boolean.valueOf(parts[4]);

            hash = computeHash();

        }

        private int computeHash() {
            int result = v;
            result = (31 * result) + conds.hashCode();
            result = 31 * result + cost;
            result = (31 * result) + tag.hashCode();
            result = 31 * result + (exact ? 1 : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NoGood nogood = (NoGood) o;
            return cost == nogood.cost && exact == nogood.exact && v == nogood.v &&
                   conds.equals(nogood.conds) && tag.equals(nogood.tag);
        }

        @Override
        public int hashCode() {
            return hash;
        }

    }

}
