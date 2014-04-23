package org.droidplanner.drone.variables.mission.commands;

import java.util.List;

import org.droidplanner.drone.variables.mission.Mission;
import org.droidplanner.drone.variables.mission.MissionItem;
import org.droidplanner.fragments.mission.MissionDetailFragment;
import com.MAVLink.Messages.ardupilotmega.msg_mission_item;
import com.MAVLink.Messages.enums.MAV_CMD;

public class SetCamTriggerDist extends MissionCMD {
	private float Distance;

	public SetCamTriggerDist(MissionItem item) {
		super(item);
		Distance = 0;
	}

	public SetCamTriggerDist(msg_mission_item msg, Mission mission) {
		super(mission);
		unpackMAVMessage(msg);
	}

	@Override
	public MissionDetailFragment getDetailFragment() {
		return null;
	}

	public float getDistance() {
		return this.Distance;
	}

	public void setDistance(float aDistance) {
		this.Distance = aDistance;
	}

	@Override
	public List<msg_mission_item> packMissionItem() {
		List<msg_mission_item> list = super.packMissionItem();
		msg_mission_item mavMsg = list.get(0);
		mavMsg.command = MAV_CMD.MAV_CMD_DO_SET_CAM_TRIGG_DIST;
		mavMsg.param1 = getDistance();
		return list;
	}

	@Override
	public void unpackMAVMessage(msg_mission_item mavMessageItem) {
		setDistance(mavMessageItem.param1);
	}
}
