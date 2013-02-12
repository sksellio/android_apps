/*
 * Copyright (C) 2013 OSRF.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.map_nav;

import java.util.concurrent.TimeUnit;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.ros.android.robotapp.RosAppActivity;
import org.ros.android.view.RosImageView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.view.VirtualJoystickView;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlListener;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.PathLayer;
import org.ros.android.view.visualization.layer.PosePublisherLayer;
import org.ros.android.view.visualization.layer.PoseSubscriberLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.time.NtpTimeProvider;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class MainActivity extends RosAppActivity {

	private static final String MAP_FRAME = "map";
	private static final String ROBOT_FRAME = "base_link";

	private RosImageView<sensor_msgs.CompressedImage> cameraView;
	private VirtualJoystickView virtualJoystickView;
	private VisualizationView mapView;
	private ViewGroup mainLayout;
	private ViewGroup sideLayout;
	private Button backButton;
	private MapPosePublisherLayer mapPosePublisherLayer;

	public MainActivity() {
		// The RosActivity constructor configures the notification title and
		// ticker
		// messages.
		super("Map nav", "Map nav");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {

		//setDefaultAppName("new_turtlebot_android_apps/map_nav");
		setDefaultAppName(null);
		setDashboardResource(R.id.top_bar);
		setMainWindowResource(R.layout.main);
		super.onCreate(savedInstanceState);
		

		cameraView = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image);
		cameraView.setTopicName("/turtlebot/application/camera/rgb/image_color/compressed_throttle");
		cameraView.setMessageType(sensor_msgs.CompressedImage._TYPE);
		cameraView.setMessageToBitmapCallable(new BitmapFromCompressedImage());
		mapView = (VisualizationView) findViewById(R.id.map_view);
		virtualJoystickView = (VirtualJoystickView) findViewById(R.id.virtual_joystick);
		virtualJoystickView.setTopicName("/cmd_vel");
		backButton = (Button) findViewById(R.id.back_button);
		

		
		
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onBackPressed();
			}
		});
		
		mapView.getCamera().jumpToFrame(ROBOT_FRAME);

		mainLayout = (ViewGroup) findViewById(R.id.main_layout);
		sideLayout = (ViewGroup) findViewById(R.id.side_layout);

	}

	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		
		super.init(nodeMainExecutor);

		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
				InetAddressFactory.newNonLoopback().getHostAddress(),
				getMasterUri());

		
		nodeMainExecutor.execute(cameraView, nodeConfiguration
				.setNodeName("camera_view"));
		nodeMainExecutor.execute(virtualJoystickView,
				nodeConfiguration.setNodeName("virtual_joystick"));

		ViewControlLayer viewControlLayer = new ViewControlLayer(this,
				nodeMainExecutor.getScheduledExecutorService(), cameraView,
				mapView, mainLayout, sideLayout);

		viewControlLayer.addListener(new CameraControlListener() {
			@Override
			public void onZoom(double focusX, double focusY, double factor) {

			}

			@Override
			public void onTranslate(float distanceX, float distanceY) {

			}

			@Override
			public void onRotate(double focusX, double focusY, double deltaAngle) {

			}
		});

		
		mapView.addLayer(viewControlLayer);
		mapView.addLayer(new OccupancyGridLayer("map"));
		mapView.addLayer(new PathLayer("/move_base/NavfnROS/plan"));
		mapPosePublisherLayer = new MapPosePublisherLayer("pose",this);
		mapView.addLayer(mapPosePublisherLayer);
		mapView.addLayer(new InitialPoseSubscriberLayer("/initialpose"));
		mapView.addLayer(new PoseSubscriberLayer("/android/goal"));
		nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
				.newNonLoopback().getHostAddress(), getMasterUri());
		NtpTimeProvider ntpTimeProvider = new NtpTimeProvider(
				InetAddressFactory.newFromHostString("192.168.0.1"),
				nodeMainExecutor.getScheduledExecutorService());
		ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES);
		nodeConfiguration.setTimeProvider(ntpTimeProvider);
		nodeMainExecutor.execute(mapView, nodeConfiguration);
	}
	
	  public void setPoseClicked(View view) {
		    setPose();
		  }

		  public void setGoalClicked(View view) {
		    setGoal();
		  }
		  
	  private void setPose() {
		  mapPosePublisherLayer.setPoseMode();
	  }
	  
	  private void setGoal() {
		  mapPosePublisherLayer.setGoalMode();
	  }
	
	  @Override
	  public boolean onCreateOptionsMenu(Menu menu){
		  menu.add(0,0,0,R.string.stop_app);
		  return super.onCreateOptionsMenu(menu);
	  }
	  
	  @Override
	  public boolean onOptionsItemSelected(MenuItem item){
		  super.onOptionsItemSelected(item);
		  switch (item.getItemId()){
		  case 0:
			  onDestroy();
			  break;
		  }
		  return true;
	  }
}