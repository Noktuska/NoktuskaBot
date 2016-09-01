package com.noktuska.bot.noktuskabot_revamped.structs.polls;

public class PollAnswer {
	
	private int votes;
	private String answer;
	
	public PollAnswer(String answer) {
		this.answer = answer;
		this.votes = 0;
	}

	public int getVotes() {
		return votes;
	}

	public void vote() {
		votes++;
	}
	
	public String getAnswer() {
		return answer;
	}
	
}
