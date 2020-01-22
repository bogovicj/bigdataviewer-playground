package sc.fiji.bdvpg.behaviour;

import bdv.BigDataViewer;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

public class ClickBehaviourInstaller
{
	private final BigDataViewer bdvHandle;
	private final Behaviour behaviour;

	public ClickBehaviourInstaller(BigDataViewer bdvHandle, ClickBehaviour behaviour )
	{
		this.bdvHandle = bdvHandle;
		this.behaviour = behaviour;
	}

	/**
	 * TODO: probably just create one behaviour for each BDV?
	 *
	 * @param name
	 * @param trigger
	 */
	public void install( String name , String trigger )
	{
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		// TODO : find this
		behaviours.install( bdvHandle.getTriggerbindings(), name );
		behaviours.behaviour( behaviour, name, trigger ) ;
	}
}
