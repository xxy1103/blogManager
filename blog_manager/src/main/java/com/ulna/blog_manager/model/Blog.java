package com.ulna.blog_manager.model;

import java.util.Arrays;

public class Blog {
    private String title;       // 博客标题
    private String date;        // 博客日期
    private String categories;  // 博客分类
    private String[] tags;      //标签
    private String saying;      //谚语
    private String content;     // 博客内容

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }
    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }
    public String getSaying() { return saying; }
    public void setSaying(String saying) { this.saying = saying; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    
    public String Info() {
        if (this.saying != null){
            return "Blog{ \n" +
                    "---\n" +
                    "title: " + title + '\n' +
                    "date: " + date + '\n' +
                    "categories: " + categories + '\n' +
                    "tags: " + Arrays.toString(tags) +'\n' + 
                    "---\n" +
                    "saying: " + saying + '\n' +
                    "<!-- more -->\n" +
                    (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : "null") + '\n' +
                    '}';
        }
        else{
            return "Blog{ \n" +
                    "---\n" +
                    "title: " + title + '\n' +
                    "date: " + date + '\n' +
                    "categories: " + categories + '\n' +
                    "tags: " + Arrays.toString(tags) +'\n' + 
                    "---\n" +
                    (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : "null") + '\n' +
                    '}';
        }
    }

    @Override
    public String toString() {
        if (this.saying != null){
            return "Blog{ \n" +
                    "---\n" +
                    "title: " + title + '\n' +
                    "date: " + date + '\n' +
                    "categories: " + categories + '\n' +
                    "tags: " + Arrays.toString(tags) +'\n' + 
                    "---\n" +
                    "saying: " + saying + '\n' +
                    "<!-- more -->\n" +
                    (content != null ? content : "null") + '\n' +
                    '}';
        }
        else{
            return "Blog{ \n" +
                    "---\n" +
                    "title: " + title + '\n' +
                    "date: " + date + '\n' +
                    "categories: " + categories + '\n' +
                    "tags: " + Arrays.toString(tags) +'\n' + 
                    "---\n" +
                    (content != null ? content : "null") + '\n' +
                    '}';
        }
    }

}
