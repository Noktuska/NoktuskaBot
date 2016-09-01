package com.noktuska.bot.noktuskabot_revamped.structs;

public class Quote {

	private String keyword;
	private String author;
	private String quote;
	
	public Quote(String keyword, String author, String quote) {
		this.keyword = keyword;
		this.author = author;
		this.quote = quote;
	}

	public void modify(String keyword, String author, String quote) {
		this.keyword = keyword;
		this.author = author;
		this.quote = quote;
	}
	
	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getQuote() {
		return quote;
	}

	public void setQuote(String quote) {
		this.quote = quote;
	}
	
	@Override
	public String toString() {
		return "\"" + quote + "\" ~" + author;
	}

}
