package acteve.explorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcolicPathsExplorer implements ExplorationStrategy
{
	private static final Logger log = LoggerFactory.getLogger(ConcolicPathsExplorer.class);
	private List<Integer> currentRoundRunIds = new ArrayList<Integer>();

	@Override
	public void perform(Config config) {
		PathQueue.addPath(new Path());
		
		log.debug("Vanilla symex with currentRoundRunIds " + Arrays.toString(currentRoundRunIds.toArray()));
		ConcolicExecutor.v().exec(currentRoundRunIds);
		
		ConcolicExecutor.v().printStats();
	}
}

