package acteve.explorer;

public interface ExplorationStrategy {
	
	public void perform(int K, String monkeyScript, boolean checkReadOnly, boolean checkIndep, boolean pruneAfterLastStep);

}
