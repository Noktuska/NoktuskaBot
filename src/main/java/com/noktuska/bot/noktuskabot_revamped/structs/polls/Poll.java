package com.noktuska.bot.noktuskabot_revamped.structs.polls;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.noktuska.bot.noktuskabot_revamped.utils.Func;

import sx.blah.discord.handle.obj.IUser;

public class Poll {
	
	private final int COLOR_SIZE = 8;
	private final Color[] COLORS = {
			new Color(255, 128, 128),
			new Color(255, 255, 128),
			new Color(128, 255, 128),
			new Color(128, 255, 255),
			new Color(128, 128, 255),
			new Color(255, 128, 255),
			new Color(160, 160, 160),
			new Color(255, 255, 255)
	};
	
	private String question;
	private List<String> userVoted = new ArrayList<String>();
	private List<PollAnswer> answers = new ArrayList<PollAnswer>();
	
	public Poll(String question, PollAnswer[] answers) {
		this.question = question;
		this.answers.addAll(Func.arrayToList(answers));
	}
	
	public void tryVote(IUser user, int answer) throws Exception {
		if (userVoted.contains(user.getID()))
			throw new Exception(user.getName() + " has already voted!");
		if (answer >= answers.size())
			throw new Exception("There are only " + answers.size() + " answers!");
		if (answer < 0)
			throw new Exception("Answer " + (answer + 1) + " is not valid... What are you trying to do?");
		answers.get(answer).vote();
		userVoted.add(user.getID());
	}
	
	public BufferedImage drawResults() {
		BufferedImage image = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D)image.getGraphics();
		
		//So no gaps are visible if the angles don't add up to exactly 360
		g.setColor(Color.BLACK);
		for (int i = 0; i < answers.size(); i++) {
			if (answers.get(i).getVotes() > 0) {
				g.setColor(COLORS[i % COLOR_SIZE]);
			}
		}
		g.fillOval(0, 0, 400, 400);
		
		g.setFont(new Font("arial", Font.BOLD, 18));
		
		int lastAngle = 0;
		for (int i = 0; i < answers.size(); i++) {
			PollAnswer elem = answers.get(i);
			
			Color curColor = COLORS[i % COLOR_SIZE];
			
			double percent = 100.0 * elem.getVotes() / (double)userVoted.size();
			percent = Math.round(percent * 100.0) / 100.0;
			int newAngle = (int)(360.0 * percent / 100.0);
			
			g.setColor(curColor);
			g.fillArc(0, 0, 400, 400, lastAngle, newAngle);
			
			g.drawString((i + 1) + ": " + elem.getAnswer() + " (" + percent + "%)", 432, 32 + i * 32);
			
			lastAngle += newAngle;
		}
		
		return image;
	}
	
	public String getResults() {
		String result = "```" + question + "\n\n";
		
		for (int i = 0; i < answers.size(); i++) {
			PollAnswer elem = answers.get(i);
			
			double percent = 100.0 * elem.getVotes() / (double)userVoted.size();
			percent = Math.round(percent * 100.0) / 100.0;
			
			result += (i + 1) + ": " + elem.getAnswer() + " (" + percent + "%)\n";
		}
		
		result += "\nParticipants: " + userVoted.size() + "```";
		
		return result;
	}
	
	@Override
	public String toString() {
		String result = "```" + question + "\n\n";
		
		for (int i = 0; i < answers.size(); i++) {
			PollAnswer elem = answers.get(i);
			
			result += (i + 1) + ": " + elem.getAnswer() + "\n";
		}
		
		result += "\nParticipants: " + userVoted.size() + "```";
		
		return result;
	}
	
}
