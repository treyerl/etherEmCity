package emcity;

import java.io.IOException;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import emcity.EmCity.EmCityController;
import emcity.EmCity.REPRESENTATION;
import emcity.EmCity.TYPE;

public class ParametersView implements IView{
	private abstract static class SliderListener implements SelectionListener{
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
		@Override
		public void widgetSelected(SelectionEvent e) {
			getSelected(((Slider) e.widget).getSelection());
		}
		
		public abstract void getSelected(int selection);
	}
	Parameters parameters;
	private boolean standalone, selfUpdate = false;
	private int x, y;
	private Label attDst, attAng, attBld, sctt, dcay, nsmpl, strength, fpf, coh, ali, sep, 
				  accd, traill, 
				  agentsCulture, agentsSquare, agentsPrivate,
				  clCulture, clSquare, clPrivate;
	private Text objExportPath, pdfExportPath, jpgExportPath;
	private Shell shell;
	private Slider attslider, angslider, bldslider, scttslider, dcayslider, nsmplslider, 
					strslider, fpfslider, cohslider, alislider, sepslider, accdslider, 
					trailSize;
	private Button stgmcheck, fpfcheck, swarmcheck, gencheck, dircheck, trailscheck, showAtt, pherocheck;
	private Button[] repr = new Button[REPRESENTATION.values().length];
	EmCityController controller;
	
	public ParametersView(Parameters p, int x, int y, EmCityController controller){
		parameters = p;
		parameters.register(this);
		this.x = x;
		this.y = y;
		this.controller = controller;
	}
	
	@Override
	public void update() {
		if (selfUpdate){
			selfUpdate = false;
			return;
		}
		attDst.setText(  parameters.getAttractionDistance()+"");
		attAng.setText(  parameters.getAtt_angle()+"");
		attBld.setText(  parameters.getAtt_factor()+"");
		sctt.setText(    parameters.getScatter()+"");
		dcay.setText(    parameters.getTrailDecay()+"");
		nsmpl.setText(   parameters.getNSamples()+"");
		strength.setText(parameters.getStigmergyStrength()+"");
		fpf.setText(     parameters.getFollowPathFactor()+"");
		coh.setText(     parameters.getCohesion()+"");
		ali.setText(     parameters.getAlignment()+"");
		sep.setText(     parameters.getSeparation()+"");
		accd.setText(    parameters.getAccessDistance()+"");
		traill.setText(  parameters.getMaxTrailLength()+"");
		
		attslider.setSelection(         parameters.getAttractionDistance());
		angslider.setSelection((int)   (parameters.getAtt_angle()*100));
		bldslider.setSelection((int)   (parameters.getAtt_factor()*100));
		scttslider.setSelection((int)  (parameters.getScatter()*100));
		dcayslider.setSelection((int)  (parameters.getTrailDecay()*100));
		nsmplslider.setSelection((int) (parameters.getNSamples()));
		strslider.setSelection((int)   (parameters.getStigmergyStrength()*100));
		fpfslider.setSelection((int)   (parameters.getFollowPathFactor()*100+400));
		cohslider.setSelection((int)    parameters.getCohesion()*100);
		alislider.setSelection((int)    parameters.getAlignment()*100);
		sepslider.setSelection((int)    parameters.getSeparation()*100);
		accdslider.setSelection((int)   parameters.getAccessDistance()*100);
		trailSize.setSelection((int)    parameters.getMaxTrailLength());
		
		showAtt.setSelection(    parameters.isShowingAttractionCircle());
		stgmcheck.setSelection(  parameters.isStigmergy());
		fpfcheck.setSelection(   parameters.isFollowingPath());
		swarmcheck.setSelection( parameters.isSwarmingOnOff());
		gencheck.setSelection(   parameters.isGeneratingVolumes());
		dircheck.setSelection(   parameters.isShowingDirection());
		trailscheck.setSelection(parameters.isShowingTrails());
		pherocheck.setSelection( parameters.isShowPheromone());
		for (REPRESENTATION r: REPRESENTATION.values()){
			repr[r.ordinal()].setSelection(parameters.isShowing(r));
		}
	}
	
	public ParametersView setLocation(Point l){
		return setLocation(l.x, l.y);
	}
	
	public ParametersView setLocation(int x, int y){
		this.x = x;
		this.y = y;
		Display.getDefault().asyncExec(() -> {
			shell.setLocation(x,y);
			shell.pack(true);
		});
		return this;
	}
	
	public void updateAgents(List<Agent> agents){
		agentsCulture.setText(agents.stream().filter(a -> a.getType() == TYPE.CULTURE).count()+"");
		agentsSquare.setText(agents.stream().filter(a -> a.getType() == TYPE.SQUARE).count()+"");
		agentsPrivate.setText(agents.stream().filter(a -> a.getType() == TYPE.PRIVATE).count()+"");
	}
	
	public void updateClusters(List<Cluster> clusters){
		clCulture.setText(clusters.stream().filter(a -> a.getType() == TYPE.CULTURE).count()+"");
		clSquare.setText(clusters.stream().filter(a -> a.getType() == TYPE.SQUARE).count()+"");
		clPrivate.setText(clusters.stream().filter(a -> a.getType() == TYPE.PRIVATE).count()+"");
	}

	@Override
	public void show(Display display) {
		shell = new Shell(display, SWT.DIALOG_TRIM);
		
		shell.setLocation(x, y);
		Layout mainLayout = new GridLayout(3, false);
		Label label;
		Button button;
		GridData middleCol = new GridData();
		middleCol.widthHint = 40;
//		GridData chckGd = new GridData();
//		chckGd.horizontalSpan = 2;
//		chckGd.widthHint = 140;
		GridData gridData120 = new GridData();
		gridData120.widthHint = 105;
				
		shell.setLayout(mainLayout);

// Building Attraction Settings
		label = new Label(shell, SWT.LEFT);
		label.setText("Building Attraction");
		final GridData attractionData = new GridData();
		attractionData.horizontalSpan = 3;
		attractionData.horizontalAlignment = SWT.FILL;
		final Group attractionGroup = new Group(shell, SWT.NONE);
		attractionGroup.setLayoutData(attractionData);
		attractionGroup.setLayout(new GridLayout(3, false));
		
		
	// ATTRACTION DISTANCE
		label = new Label(attractionGroup, SWT.LEFT);
		label.setText("Attraction Distance");
		label.setLayoutData(gridData120);
		attDst = new Label(attractionGroup, SWT.RIGHT);
		attDst.setLayoutData(middleCol);
		attDst.setText(parameters.getAttractionDistance()+"");
		attslider = new Slider(attractionGroup, SWT.LEFT);
		attslider.setMaximum(500+attslider.getThumb());
		attslider.setMinimum(100);
		attslider.setSelection(parameters.getAttractionDistance());
		attslider.addSelectionListener(new SliderListener(){
			@Override
			public void getSelected(int selection){
				parameters.setAttractionDistance(selection);
				attDst.setText(selection+"");
				selfUpdate = true;
			}
		});
		
	// ATTRACTION ANGLE
		label = new Label(attractionGroup, SWT.LEFT);
		label.setText("Attraction Angle");
		label.setLayoutData(gridData120);
		attAng = new Label(attractionGroup, SWT.RIGHT);
		attAng.setLayoutData(middleCol);
		attAng.setText(parameters.getAtt_angle()+"");
		angslider = new Slider(attractionGroup, SWT.LEFT);
		angslider.setMaximum(312+angslider.getThumb());
		angslider.setMinimum(140);
		angslider.setSelection((int) (parameters.getAtt_angle()*100));
		angslider.addSelectionListener(new SliderListener(){
			@Override
			public void getSelected(int selection) {
				float angle = ((float) selection)/100;
				parameters.setAtt_angle(angle);
				attAng.setText(angle+"");
				selfUpdate = true;
			}
		});
		
	// ATTRACTION BUILDINGS
		label = new Label(attractionGroup, SWT.LEFT);
		label.setText("Attraction Buildings");
		label.setLayoutData(gridData120);
		attBld = new Label(attractionGroup, SWT.RIGHT);
		attBld.setLayoutData(middleCol);
		attBld.setText(parameters.getAtt_factor()+"");
		bldslider = new Slider(attractionGroup, SWT.LEFT);
		bldslider.setMinimum(10);
		bldslider.setMaximum(100+bldslider.getThumb());
		bldslider.setSelection((int) (parameters.getAtt_factor()*100));
		bldslider.addSelectionListener(new SliderListener(){
			@Override
			public void getSelected(int selection) {
				float factor = ((float) selection)/100;
				parameters.setAtt_factor(factor);
				attBld.setText(factor+"");
				selfUpdate = true;
			}
		});
		
	// SHOWING ATTRACTION CIRCLE
		label = new Label(shell, SWT.LEFT);
		label.setText("Attraction Circle");
//		label.setLayoutData(gridData120);
		label = new Label(shell, SWT.RIGHT);
		label.setText("[C]");
		showAtt = new Button(shell, SWT.CHECK);
		showAtt.setSelection(parameters.isShowingAttractionCircle());
		showAtt.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				boolean att = !parameters.isShowingAttractionCircle();
				parameters.setShowingAttractionCircle(att);
				controller.showAttractionCircle(att);
				selfUpdate = true;
			}
		});
		
		
// stigmergy
		boolean stigm = parameters.isStigmergy();
		label = new Label(shell, SWT.LEFT);
		label.setText("Stigmergy On/Off");
		label = new Label(shell, SWT.RIGHT);
		label.setText("[M]");
		stgmcheck = new Button(shell, SWT.CHECK);
		stgmcheck.setSelection(stigm);
		final GridData stigmergyData = new GridData();
		stigmergyData.horizontalSpan = 3;
		stigmergyData.horizontalAlignment = SWT.FILL;
		final Group stigmergyGroup = new Group(shell, SWT.NONE);
		stigmergyGroup.setLayoutData(stigmergyData);
		stigmergyGroup.setVisible(stigm);
        stigmergyGroup.setLayout(new GridLayout(3, false));
			// scatter
	        label = new Label(stigmergyGroup, SWT.LEFT);
			label.setText("Scatter");
			label.setLayoutData(gridData120);
			sctt = new Label(stigmergyGroup, SWT.RIGHT);
			sctt.setLayoutData(middleCol);
			sctt.setText(parameters.getScatter()+"");
			scttslider = new Slider(stigmergyGroup, SWT.LEFT);
			scttslider.setMaximum(1500+scttslider.getThumb());
			scttslider.setMinimum(100);
			scttslider.setSelection((int) (parameters.getScatter()*100));
			scttslider.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					float factor = ((float) selection)/100;
					parameters.setScatter(factor);
					sctt.setText(factor+"");
					selfUpdate = true;
				}
			});
			// decay
			label = new Label(stigmergyGroup, SWT.LEFT);
			label.setText("Decay");
			dcay = new Label(stigmergyGroup, SWT.RIGHT);
			dcay.setLayoutData(middleCol);
			dcay.setText(parameters.getTrailDecay()+"");
			dcayslider = new Slider(stigmergyGroup, SWT.LEFT);
			dcayslider.setMinimum(75);
			dcayslider.setMaximum(200+dcayslider.getThumb());
			dcayslider.setSelection((int) (parameters.getTrailDecay()*100));
			dcayslider.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					float factor = ((float) selection)/100;
					parameters.setTrailDecay(factor);
					dcay.setText(factor+"");
					selfUpdate = true;
				}
			});
			// nsamples
			label = new Label(stigmergyGroup, SWT.LEFT);
			label.setText("NSamples");
			nsmpl = new Label(stigmergyGroup, SWT.RIGHT);
			nsmpl.setLayoutData(middleCol);
			nsmpl.setText(parameters.getNSamples()+"");
			nsmplslider = new Slider(stigmergyGroup, SWT.LEFT);
			nsmplslider.setMinimum(5);
			nsmplslider.setMaximum(255+nsmplslider.getThumb());
			nsmplslider.setSelection((int) (parameters.getNSamples()));
			nsmplslider.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					float factor = ((float) selection);
					parameters.setTrailDecay(factor);
					nsmpl.setText(factor+"");
					selfUpdate = true;
				}
			});
			// strength
			label = new Label(stigmergyGroup, SWT.LEFT);
			label.setText("Strength");
			strength = new Label(stigmergyGroup, SWT.RIGHT);
			strength.setLayoutData(middleCol);
			strength.setText(parameters.getStigmergyStrength()+"");
			strslider = new Slider(stigmergyGroup, SWT.LEFT);
			strslider.setMinimum(0);
			strslider.setMaximum(200+strslider.getThumb());
			strslider.setSelection((int) (parameters.getStigmergyStrength() * 100));
			strslider.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					float factor = ((float) selection/100);
					parameters.setStigmergyStrength(factor);
					strength.setText(factor+"");
					selfUpdate = true;
				}
			});
		stgmcheck.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				boolean stigm = !parameters.isStigmergy();
				parameters.setStigmergy(stigm);
				stigmergyGroup.setVisible(stigm);
				stigmergyData.exclude = !stigm;
				shell.pack(true);
				selfUpdate = true;
			}
		});
		
// follow Path
		boolean follow = parameters.isFollowingPath();
		label = new Label(shell, SWT.LEFT);
		label.setText("Follow Path");
		label = new Label(shell, SWT.RIGHT);
		label.setText("[P]");
		fpfcheck = new Button(shell, SWT.CHECK);
		fpfcheck.setSelection(follow);
		
		final GridData followPathData = new GridData();
		followPathData.horizontalSpan = 3;
		followPathData.exclude = !follow;
		followPathData.horizontalAlignment = SWT.FILL;
		final Group followPathGroup = new Group(shell, SWT.NONE);
		followPathGroup.setVisible(follow);
		followPathGroup.setLayoutData(followPathData);
		followPathGroup.setLayout(new GridLayout(3, false));
			// follow path factor
			label = new Label(followPathGroup, SWT.LEFT);
			label.setText("Strength");
			label.setLayoutData(gridData120);
			fpf = new Label(followPathGroup, SWT.RIGHT);
			fpf.setLayoutData(middleCol);
			fpf.setText(parameters.getFollowPathFactor()+"");
			fpfslider = new Slider(followPathGroup, SWT.LEFT);
			
			fpfslider.setMaximum(800+fpfslider.getThumb());
			fpfslider.setMinimum(0);
			fpfslider.setSelection((int) (parameters.getFollowPathFactor() * 100 + 400));
			fpfslider.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					float factor = ((float) (selection - 400)/100);
					parameters.setFollowPathFactor(factor);
					fpf.setText(factor+"");
					selfUpdate = true;
				}
			});
		
		fpfcheck.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				boolean follow = !parameters.isFollowingPath();
				parameters.setFollowingPath(follow);
				followPathGroup.setVisible(follow);
				followPathData.exclude = !follow;
				shell.pack(true);
				selfUpdate = true;
			}
		});
		
// swarming on / off
		boolean isSwarming = parameters.isSwarmingOnOff();
		label = new Label(shell, SWT.LEFT);
		label.setText("Swarming On / Off");
		label = new Label(shell, SWT.RIGHT);
		label.setText("[S]");
		swarmcheck = new Button(shell, SWT.CHECK);
		swarmcheck.setSelection(isSwarming);
		
		final GridData swarmingData = new GridData();
		swarmingData.horizontalSpan = 3;
		swarmingData.exclude = !isSwarming;
		swarmingData.horizontalAlignment = SWT.FILL;
		final Group swarmingGroup = new Group(shell, SWT.NONE);
		swarmingGroup.setVisible(isSwarming);
		swarmingGroup.setLayoutData(swarmingData);
		swarmingGroup.setLayout(new GridLayout(3, false));
			// cohesion
			label = new Label(swarmingGroup, SWT.LEFT);
			label.setText("Cohesion");
			label.setLayoutData(gridData120);
			coh = new Label(swarmingGroup, SWT.RIGHT);
			coh.setLayoutData(middleCol);
			coh.setText(parameters.getCohesion()+"");
			cohslider = new Slider(swarmingGroup, SWT.LEFT);
			cohslider.setMinimum(0);
			cohslider.setMaximum(300+cohslider.getThumb());
			cohslider.setSelection((int) (parameters.getCohesion() * 100));
			cohslider.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					float factor = ((float) selection/100);
					parameters.setCohesion(factor);
					coh.setText(factor+"");
					selfUpdate = true;
				}
			});
			
			// alignment
			label = new Label(swarmingGroup, SWT.LEFT);
			label.setText("Alignment");
			ali = new Label(swarmingGroup, SWT.RIGHT);
			ali.setLayoutData(middleCol);
			ali.setText(parameters.getAlignment()+"");
			alislider = new Slider(swarmingGroup, SWT.LEFT);
			alislider.setMinimum(0);
			alislider.setMaximum(300+alislider.getThumb());
			alislider.setSelection((int) (parameters.getAlignment() * 100));
			alislider.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					float factor = ((float) selection/100);
					parameters.setAlignment(factor);
					ali.setText(factor+"");
					selfUpdate = true;
				}
			});
			
			// separation
			label = new Label(swarmingGroup, SWT.LEFT);
			label.setText("Separation");
			sep = new Label(swarmingGroup, SWT.RIGHT);
			sep.setLayoutData(middleCol);
			sep.setText(parameters.getSeparation()+"");
			sepslider = new Slider(swarmingGroup, SWT.LEFT);
			sepslider.setMinimum(0);
			sepslider.setMaximum(300+sepslider.getThumb());
			sepslider.setSelection((int) (parameters.getSeparation() * 100));
			sepslider.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					float factor = ((float) selection/100);
					parameters.setSeparation(factor);
					sep.setText(factor+"");
					selfUpdate = true;
				}
			});
		
		
		swarmcheck.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				boolean isSwarming = !parameters.isSwarmingOnOff();
				parameters.setSwarmingOnOff(isSwarming);
				swarmingGroup.setVisible(isSwarming);
				swarmingData.exclude = !isSwarming;
				shell.pack(true);
				selfUpdate = true;
			}
		});
		
// GENERATE VOLUMES
		boolean generate = parameters.isGeneratingVolumes();
		label = new Label(shell, SWT.LEFT);
		label.setText("Generate Volumes");
		label = new Label(shell, SWT.RIGHT);
		label.setText("[G]");
		gencheck = new Button(shell, SWT.CHECK);
		gencheck.setSelection(generate);
		
		final GridData generateData = new GridData();
		generateData.horizontalSpan = 3;
		generateData.exclude = !generate;
		generateData.horizontalAlignment = SWT.FILL;
		final Group generateGroup = new Group(shell, SWT.NONE);
		generateGroup.setVisible(generate);
		generateGroup.setLayoutData(generateData);
		generateGroup.setLayout(new GridLayout(3, false));
			// accessible distance
			label = new Label(generateGroup, SWT.LEFT);
			label.setText("Access Distance");
			label.setLayoutData(gridData120);
			accd = new Label(generateGroup, SWT.RIGHT);
			accd.setLayoutData(middleCol);
			accd.setText(parameters.getAccessDistance()+"");
			accdslider = new Slider(generateGroup, SWT.LEFT);
			accdslider.setMaximum(1000+accdslider.getThumb());
			accdslider.setMinimum(100);
			accdslider.setSelection((int) (parameters.getAccessDistance()));
			accdslider.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					parameters.setAccessDistance(selection);
					accd.setText(selection+"");
					selfUpdate = true;
				}
			});
		
		gencheck.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				boolean gen = !parameters.isGeneratingVolumes();
				parameters.setGeneratingVolumes(gen);
				generateGroup.setVisible(gen);
				generateData.exclude = !gen;
				shell.pack(true);
				selfUpdate = true;
			}
		});
		
// SHOW TRAILS
		boolean showingTrails = parameters.isShowingTrails();
		label = new Label(shell, SWT.LEFT);
		label.setText("Show Trails");
		label = new Label(shell, SWT.RIGHT);
		label.setText("[X]");
		trailscheck = new Button(shell, SWT.CHECK);
		trailscheck.setSelection(showingTrails);
		final GridData trailData = new GridData();
		trailData.horizontalSpan = 3;
		trailData.exclude = !showingTrails;
		trailData.horizontalAlignment = SWT.FILL;
		final Group trailGroup = new Group(shell, SWT.NONE);
		trailGroup.setVisible(showingTrails);
		trailGroup.setLayoutData(trailData);
		trailGroup.setLayout(new GridLayout(3, false));
			// accessible distance
			label = new Label(trailGroup, SWT.LEFT);
			label.setText("Trail Length");
			label.setLayoutData(gridData120);
			traill = new Label(trailGroup, SWT.RIGHT);
			traill.setLayoutData(middleCol);
			traill.setText(parameters.getMaxTrailLength()+"");
			trailSize = new Slider(trailGroup, SWT.LEFT);
			trailSize.setMaximum(2000+trailSize.getThumb());
			trailSize.setMinimum(100);
			trailSize.setSelection((int) (parameters.getMaxTrailLength()));
			trailSize.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					parameters.setMaxTrailLength(selection);
					traill.setText(selection+"");
					selfUpdate = true;
				}
			});
		
		trailscheck.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				boolean showingTrails = !parameters.isShowingTrails();
				parameters.setShowingTrails(showingTrails);
				controller.toggleShowTrails();
				trailGroup.setVisible(showingTrails);
				trailData.exclude = !showingTrails;
				shell.pack(true);
				selfUpdate = true;
			}
		});
		
// SHOW Direction
		boolean showingDirs = parameters.isShowingDirection();
		label = new Label(shell, SWT.LEFT);
		label.setText("Show Direction");
		label = new Label(shell, SWT.RIGHT);
		label.setText("[D]");
		dircheck = new Button(shell, SWT.CHECK);
		dircheck.setSelection(showingDirs);
		dircheck.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				boolean gen = !parameters.isShowingDirection();
				parameters.setShowingDirection(gen);
				selfUpdate = true;
			}
		});

// REPRESENTATION
		GridData two = new GridData();
		two.horizontalSpan = 2;
		two.widthHint = 200;
		
		for (REPRESENTATION r: REPRESENTATION.values()){
			label = new Label(shell, SWT.LEFT);
			label.setText("Show "+r.name().toLowerCase());
			label = new Label(shell, SWT.RIGHT);
			label.setText("["+r.getKeyStroke()+"]");
			repr[r.ordinal()] = new Button(shell, SWT.CHECK);
			repr[r.ordinal()].setSelection(parameters.isShowing(r));
			repr[r.ordinal()].addListener(SWT.Selection, new Listener(){
				@Override
				public void handleEvent(Event event) {
					parameters.setShowing(r, !parameters.isShowing(r));
					controller.geometryVisibility(r);
					selfUpdate = true;
				}
			});
		}
		
		boolean showPheromones = parameters.isShowPheromone();
		label = new Label(shell, SWT.LEFT);
		label.setText("Show Pheromones");
		label = new Label(shell, SWT.RIGHT);
		label.setText("[H]");
		pherocheck = new Button(shell, SWT.CHECK);
		pherocheck.setSelection(showPheromones);
		pherocheck.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				boolean gen = !parameters.isShowPheromone();
				parameters.setShowPheromone(gen);
				selfUpdate = true;
			}
		});
		
// BUTTONS		
		final GridData buttonsData = new GridData();
		buttonsData.horizontalSpan = 3;
		buttonsData.horizontalAlignment = SWT.FILL;
		final Group buttonsGroup = new Group(shell, SWT.NONE);
		buttonsGroup.setLayoutData(buttonsData);
		buttonsGroup.setLayout(new GridLayout(3, false));
		
		String[] strokes = new String[]{"U","Q","I"};
		for(TYPE t: TYPE.values()){
			button = new Button(buttonsGroup, SWT.PUSH);
			button.setText(t.name()+" ["+strokes[t.ordinal()]+"]");
			button.addListener(SWT.Selection, new Listener(){
				@Override
				public void handleEvent(Event event) {
					controller.toggleVolumes(t.ordinal());
				}
			});
		}
		
		button = new Button(buttonsGroup, SWT.PUSH);
		button.setText("Agents [A]");
		button.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				controller.addAgents();
			}
		});
		
		button = new Button(buttonsGroup, SWT.PUSH);
		button.setText("Typology [T]");
		button.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				try {
					controller.updateTypologies();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		button = new Button(buttonsGroup, SWT.PUSH);
		button.setText("Move Origins [O]");
		button.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				controller.moveOrigins();
			}
		});
		
		
// Export Obj
		GridData file = new GridData();
		file.widthHint = 200;
		
		button = new Button(shell, SWT.PUSH);
		button.setText("Export OBJ");
		button.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				System.out.println("export OBJ");
			}
		});
		final GridData objData = new GridData();
		objData.horizontalSpan = 2;
		objData.horizontalAlignment = SWT.FILL;
		final Group objGroup = new Group(shell, SWT.NONE);
		objGroup.setLayoutData(objData);
		objGroup.setLayout(new GridLayout(2, false));
		objExportPath = new Text(objGroup, SWT.LEFT);
		objExportPath.setText("/asdf/asdf/asdf");
		objExportPath.setLayoutData(file);
		objExportPath.addListener(SWT.MenuDetect, new Listener(){
			@Override
			public void handleEvent(Event event) {
				DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
				dialog.setFilterPath(parameters.getPathOBJ());
				String result = dialog.open();
				if (result != null) {
					parameters.setPathOBJ(result);
					objExportPath.setText(result);
				}
			}
		});
		button = new Button(objGroup, SWT.PUSH);
		button.setText("Choose");
		
// Export PDF
		button = new Button(shell, SWT.PUSH);
		button.setText("Export PDF");
		button.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				System.out.println("export PDF");
			}
		});
		final GridData pdfData = new GridData();
		pdfData.horizontalSpan = 2;
		pdfData.horizontalAlignment = SWT.FILL;
		final Group pdfGroup = new Group(shell, SWT.NONE);
		pdfGroup.setLayoutData(pdfData);
		pdfGroup.setLayout(new GridLayout(2, false));
		pdfExportPath = new Text(pdfGroup, SWT.LEFT);
		pdfExportPath.setText("/asdf/asdf/asdf");
		pdfExportPath.setLayoutData(file);
		pdfExportPath.addListener(SWT.MenuDetect, new Listener(){
			@Override
			public void handleEvent(Event event) {
				DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
				dialog.setFilterPath(parameters.getPathPDF());
				String result = dialog.open();
				if (result != null) {
					parameters.setPathPDF(result);
					pdfExportPath.setText(result);
				}
				
			}
		});
		button = new Button(pdfGroup, SWT.PUSH);
		button.setText("Choose");
		
// Export JPG
		button = new Button(shell, SWT.PUSH);
		button.setText("Export JPG");
		button.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				System.out.println("export JPG");
			}
		});
		final GridData jpgData = new GridData();
		jpgData.horizontalSpan = 2;
		jpgData.horizontalAlignment = SWT.FILL;
		final Group jpgGroup = new Group(shell, SWT.NONE);
		jpgGroup.setLayoutData(jpgData);
		jpgGroup.setLayout(new GridLayout(2, false));
		jpgExportPath = new Text(jpgGroup, SWT.LEFT);
		jpgExportPath.setText("/asdf/asdf/asdf");
		jpgExportPath.setLayoutData(file);
		jpgExportPath.addListener(SWT.MenuDetect, new Listener(){
			@Override
			public void handleEvent(Event event) {
				DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
				dialog.setFilterPath(parameters.getPathJPG());
				String result = dialog.open();
				if (result != null) {
					parameters.setPathJPG(result);
					jpgExportPath.setText(result);
				}
				
			}
		});
		button = new Button(jpgGroup, SWT.PUSH);
		button.setText("Choose");
		
// Agents
		GridData num = new GridData();
		num.widthHint = 30;
		GridData typeData = new GridData();
		typeData.widthHint = 70;
		final GridData statData = new GridData();
		statData.horizontalSpan = 3;
		statData.horizontalAlignment = SWT.FILL;
		final Group statGroup = new Group(shell, SWT.NONE);
		statGroup.setText("Statistcis");
		statGroup.setLayoutData(statData);
		statGroup.setLayout(new GridLayout(7, false));
		label = new Label(statGroup, SWT.RIGHT);
		label.setText("Agents ");
		label.setLayoutData(typeData);
		label = new Label(statGroup, SWT.RIGHT);
		label.setText("culture: ");
		agentsCulture = new Label(statGroup, SWT.LEFT);
		agentsCulture.setText("0");
		agentsCulture.setLayoutData(num);
		label = new Label(statGroup, SWT.RIGHT);
		label.setText("square: ");
		agentsSquare = new Label(statGroup, SWT.LEFT);
		agentsSquare.setText("0");
		agentsSquare.setLayoutData(num);
		label = new Label(statGroup, SWT.RIGHT);
		label.setText("private: ");
		agentsPrivate = new Label(statGroup, SWT.LEFT);
		agentsPrivate.setText("0");
		agentsPrivate.setLayoutData(num);
		
// Clusters
		label = new Label(statGroup, SWT.RIGHT);
		label.setText("Built Clusters: ");
		label.setLayoutData(typeData);
		label = new Label(statGroup, SWT.RIGHT);
		label.setText("culture: ");
		clCulture = new Label(statGroup, SWT.LEFT);
		clCulture.setText("0");
		clCulture.setLayoutData(num);
		label = new Label(statGroup, SWT.RIGHT);
		label.setText("square: ");
		clSquare = new Label(statGroup, SWT.LEFT);
		clSquare.setText("0");
		clSquare.setLayoutData(num);
		label = new Label(statGroup, SWT.RIGHT);
		label.setText("private: ");
		clPrivate = new Label(statGroup, SWT.LEFT);
		clPrivate.setText("0");
		clPrivate.setLayoutData(num);
		
		// - - - - - - - - - - -
		shell.setText("EmCity Parameters");
        shell.pack();
        shell.open();
        
        if (standalone){
        	 while (!shell.isDisposed()) {
        		 if (!display.readAndDispatch()) {
        			 display.sleep();
        		 }
        	 }
        } else {
        	shell.addListener(SWT.Close, new Listener() { 
        		@Override 
        		public void handleEvent(Event event){ 
        			System.out.println("Child Shell handling Close event, about to dispose this Shell"); 
        			shell.dispose(); 
        		} 
            });
        }
	}
}
