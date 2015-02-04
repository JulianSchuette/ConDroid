package acteve.explorer;

import java.util.ArrayList;
import java.util.List;

public class ConcolicPathsExplorer implements ExplorationStrategy
{
	private List<Integer> currentRoundRunIds = new ArrayList<Integer>();

	@Override
	public void perform(Config config) {
		PathQueue.addPath(new Path());
		
		System.out.println("Vanilla symex with currentRoundRunIds " + currentRoundRunIds.toArray().toString());
		ConcolicExecutor.v().exec(currentRoundRunIds);
		
		ConcolicExecutor.v().printStats();
	}
}

