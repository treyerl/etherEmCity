package emcity;

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
	private boolean standalone;
	private int x, y;
	private Label attDst, attAng, attBld, sctt, dcay, nsmpl, strength, fpf, coh, ali, sep, accd;
	private Text objExportPath, pdfExportPath, jpgExportPath;
	private Shell shell;
	private Slider attslider, angslider, bldslider, scttslider, dcayslider, nsmplslider, strslider, 
					fpfslider, cohslider, alislider, sepslider, accdslider, trailSize;
	private Button stgmcheck, fpfcheck, swarmcheck, gencheck, dircheck, trailscheck;
	EmCityController controller;
	
	public ParametersView(Parameters p, int x, int y, EmCityController controller){
		parameters = p;
		this.x = x;
		this.y = y;
		this.controller = controller;
	}
	
	@Override
	public void update() {
		// TODO Auto-generated method stub
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

	@Override
	public void show(Display display) {
		shell = new Shell(display, SWT.DIALOG_TRIM);
		
		shell.setLocation(x, y);
		Layout mainLayout = new GridLayout(3, false);
		Label label;
		Button check;
		GridData middleCol = new GridData();
		middleCol.widthHint = 40;
		GridData chckGd = new GridData();
		chckGd.horizontalSpan = 2;
		chckGd.widthHint = 140;
		GridData gridData120 = new GridData();
		gridData120.widthHint = 105;
				
		shell.setLayout(mainLayout);

// General Settings
	// ATTRACTION DISTANCE
		label = new Label(shell, SWT.LEFT);
		label.setText("Attraction Distance");
		attDst = new Label(shell, SWT.RIGHT);
		attDst.setLayoutData(middleCol);
		attDst.setText(parameters.getAttractionDistance()+"");
		attslider = new Slider(shell, SWT.LEFT);
		attslider.setMaximum(500+attslider.getThumb());
		attslider.setMinimum(100);
		attslider.setSelection(parameters.getAttractionDistance());
		attslider.addSelectionListener(new SliderListener(){
			@Override
			public void getSelected(int selection){
				parameters.setAttractionDistance(selection);
				attDst.setText(selection+"");
			}
		});
		
	// ATTRACTION ANGLE
		label = new Label(shell, SWT.LEFT);
		label.setText("Attraction Angle");
		attAng = new Label(shell, SWT.RIGHT);
		attAng.setLayoutData(middleCol);
		attAng.setText(parameters.getAtt_angle()+"");
		angslider = new Slider(shell, SWT.LEFT);
		angslider.setMaximum(312+angslider.getThumb());
		angslider.setMinimum(140);
		angslider.setSelection((int) (parameters.getAtt_angle()*100));
		angslider.addSelectionListener(new SliderListener(){
			@Override
			public void getSelected(int selection) {
				float angle = ((float) selection)/100;
				parameters.setAtt_angle(angle);
				attAng.setText(angle+"");
			}
		});
		
	// ATTRACTION BUILDINGS
		label = new Label(shell, SWT.LEFT);
		label.setText("Attraction Buildings");
		attBld = new Label(shell, SWT.RIGHT);
		attBld.setLayoutData(middleCol);
		attBld.setText(parameters.getAtt_factor()+"");
		bldslider = new Slider(shell, SWT.LEFT);
		bldslider.setMinimum(10);
		bldslider.setMaximum(100+bldslider.getThumb());
		bldslider.setSelection((int) (parameters.getAtt_factor()*100));
		bldslider.addSelectionListener(new SliderListener(){
			@Override
			public void getSelected(int selection) {
				float factor = ((float) selection)/100;
				parameters.setAtt_factor(factor);
				attBld.setText(factor+"");
			}
		});
		
		
// stigmergy
		boolean stigm = parameters.isStigmergy();
		label = new Label(shell, SWT.LEFT);
		label.setText("Stigmergy On/Off");
		label.setLayoutData(chckGd);
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
				
			}
		});
		
// follow Path
		boolean follow = parameters.isFollowingPath();
		label = new Label(shell, SWT.LEFT);
		label.setText("Follow Path");
		label.setLayoutData(chckGd);
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
			}
		});
		
// swarming on / off
		boolean isSwarming = parameters.isSwarmingOnOff();
		label = new Label(shell, SWT.LEFT);
		label.setText("Swarming On / Off");
		label.setLayoutData(chckGd);
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
			}
		});
		
// GENERATE VOLUMES
		boolean generate = parameters.isGeneratingVolumes();
		label = new Label(shell, SWT.LEFT);
		label.setText("Generate Volumes");
		label.setLayoutData(chckGd);
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
			accd.setText(parameters.getAttractionDistance()+"");
			accdslider = new Slider(generateGroup, SWT.LEFT);
			accdslider.setMaximum(1000+accdslider.getThumb());
			accdslider.setMinimum(100);
			accdslider.setSelection((int) (parameters.getAttractionDistance()));
			accdslider.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					parameters.setAttractionDistance(selection);
					accd.setText(selection+"");
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
			}
		});
		
// SHOW TRAILS
		boolean showingTrails = parameters.isShowingTrails();
		label = new Label(shell, SWT.LEFT);
		label.setText("Show Trails");
		label.setLayoutData(chckGd);
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
			accd = new Label(trailGroup, SWT.RIGHT);
			accd.setLayoutData(middleCol);
			accd.setText(parameters.getMaxTrailLength()+"");
			trailSize = new Slider(trailGroup, SWT.LEFT);
			trailSize.setMaximum(2000+trailSize.getThumb());
			trailSize.setMinimum(100);
			trailSize.setSelection((int) (parameters.getMaxTrailLength()));
			trailSize.addSelectionListener(new SliderListener(){
				@Override
				public void getSelected(int selection) {
					parameters.setMaxTrailLength(selection);
					accd.setText(selection+"");
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
			}
		});
		
// SHOW Direction
		boolean showingDirs = parameters.isShowingDirection();
		label = new Label(shell, SWT.LEFT);
		label.setText("Show Direction");
		label.setLayoutData(chckGd);
		dircheck = new Button(shell, SWT.CHECK);
		dircheck.setSelection(showingDirs);
		dircheck.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				boolean gen = !parameters.isShowingDirection();
				parameters.setShowingDirection(gen);
			}
		});
		
		GridData two = new GridData();
		two.horizontalSpan = 2;
		two.widthHint = 200;
		
// Export Obj
		check = new Button(shell, SWT.PUSH);
		check.setText("Export OBJ");
		check.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				System.out.println("export OBJ");
			}
		});
		objExportPath = new Text(shell, SWT.LEFT);
		objExportPath.setText("/asdf/asdf/asdf");
		objExportPath.setLayoutData(two);
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
		
// Export PDF
		check = new Button(shell, SWT.PUSH);
		check.setText("Export PDF");
		check.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				System.out.println("export PDF");
			}
		});
		pdfExportPath = new Text(shell, SWT.LEFT);
		pdfExportPath.setText("/asdf/asdf/asdf");
		pdfExportPath.setLayoutData(two);
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
		
// Export JPG
		check = new Button(shell, SWT.PUSH);
		check.setText("Export JPG");
		check.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				System.out.println("export JPG");
			}
		});
		jpgExportPath = new Text(shell, SWT.LEFT);
		jpgExportPath.setText("/asdf/asdf/asdf");
		jpgExportPath.setLayoutData(two);
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
