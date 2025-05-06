package com.ulna.blog_manager.model;

import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import org.yaml.snakeyaml.Yaml;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;


public class Blog implements Comparable<Blog>,Cloneable {
    private static final String FRONT_MATTER_DELIMITER = "---";
    private static final String MORE_TAG = "<!-- more -->";
    
    private Path filepath;    // 博客路径
    private String filename;  // 博客文件名
    private String title;       // 博客标题
    private LocalDateTime dateTime;        // 博客日期
    private String categories;  // 博客分类
    private String[] tags;      //标签
    private String saying;      //谚语
    private String content;     //博客内容

    // 用于解析日期时间字符串的格式化器
    // 用于解析日期时间字符串的格式化器 - 更灵活的格式
    private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("yyyy-M-d H:m:s");
    // 创建自定义格式的 DateTimeFormatter
    private static final DateTimeFormatter formatterPrint = DateTimeFormatter.ofPattern("'_'yyyyMMdd'_'HHmmss");
    @Override
    public Blog clone() throws CloneNotSupportedException {
        Blog cloned = (Blog) super.clone(); // 利用Object.clone()创建浅拷贝
        
        // 然后只需要对可变字段进行深拷贝
        if (this.tags != null) {
            cloned.tags = this.tags.clone();
        }
        
        return cloned;
    }
    
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Blog other = (Blog) obj;
        return filename.equals(other.filename);
    }
    public int hashCode() {
        return filename.hashCode();
    }

    /**
     * 默认无参构造方法
     */
    public Blog() {}
    
    public Blog(
        String title,
        String categories,
        String[] tags,
        String saying) {
            this.title = title;
            this.categories = categories;
            this.tags = tags;
            this.saying = saying;
            this.dateTime = LocalDateTime.now(); // 默认当前时间
            this.content = null; // 默认内容为空
            this.filename = title+this.dateTime.format(formatterPrint)+".md"; // 默认文件名
            this.filepath = null;           
        }
    public Blog(
        String title,
        String categories,
        String[] tags,
        String saying,
        LocalDateTime dateTime) {
            this.title = title;
            this.categories = categories;
            this.tags = tags;
            this.saying = saying;
            this.dateTime = dateTime; // 默认当前时间
            this.content = null; // 默认内容为空
            this.filename = null; // 默认文件名
            this.filepath = null;           
        }
    /**
     * 从文件内容构造Blog对象的构造方法
     * 
     * @param fileContent 文件的完整字符串内容。
     * @throws IllegalArgumentException 如果文件格式不正确（缺少分隔符等）。
     */
    public Blog(String fileContent,Path path) {
        this.filepath = path;
        this.filename = path.getFileName().toString(); // 获取文件名
        // --- 解析 Front-matter 部分 (YAML) ---
        if (fileContent == null || !fileContent.startsWith(FRONT_MATTER_DELIMITER)) {
            throw new IllegalArgumentException("无效的文件内容：必须以 '---' 开头。");
        }

        // 查找第二个分隔符的位置
        int secondDelimiterStart = fileContent.indexOf(FRONT_MATTER_DELIMITER + "\n", FRONT_MATTER_DELIMITER.length());
        // 兼容 Windows 换行符
        if (secondDelimiterStart == -1) {
            secondDelimiterStart = fileContent.indexOf(FRONT_MATTER_DELIMITER + "\r\n", FRONT_MATTER_DELIMITER.length());
        }

        if (secondDelimiterStart == -1) {
            throw new IllegalArgumentException("无效的文件内容：未找到第二个 '---' 分隔符。");
        }

        // 提取 Front-matter 部分 (YAML)
        // +1 是为了跳过第一个分隔符后的换行符
        String yamlPart = fileContent.substring(FRONT_MATTER_DELIMITER.length() + 1, secondDelimiterStart).trim();

        // 提取内容部分 (分隔符之后的所有内容)
        // +1 是为了跳过第二个分隔符后的换行符
        String remainingContentPart = fileContent.substring(secondDelimiterStart + FRONT_MATTER_DELIMITER.length() + 1).trim();

        // --- 解析 YAML Front-matter ---
        Yaml yaml = new Yaml();
        try {
            Map<String, Object> frontMatter = yaml.load(yamlPart);

            if (frontMatter != null) {
                this.title = (String) frontMatter.get("title");
                // 日期可能是 Date 对象或 String，这里先统一转为 String
                Object dateObj = frontMatter.get("date");
                String dateValue = formatter.format(((java.util.Date) dateObj).toInstant()
                                            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                
                this.dateTime = dateValue != null ?LocalDateTime.parse(dateValue, Blog.formatter) : null;
                this.categories = (String) frontMatter.get("categories");

                // 处理 Tags (可能是 List)
                Object tagsValue = frontMatter.get("tags");
                if (tagsValue instanceof List) {
                    @SuppressWarnings("unchecked") // SnakeYAML 返回 List<Object>，我们期望是 String
                    List<Object> tagList = (List<Object>) tagsValue;
                    // 转换为 String 数组
                    List<String> stringTags = new ArrayList<>();
                    for (Object tag : tagList) {
                        if (tag != null) {
                            stringTags.add(tag.toString());
                        }
                    }
                    this.tags = stringTags.toArray(new String[0]);

                } else if (tagsValue != null) {
                    // 如果不是 List 但存在，尝试转为 String 并放入单元素数组（或记录警告）
                    this.tags = new String[]{tagsValue.toString()};
                    System.err.println("警告：Tags 格式可能不正确，期望是列表，实际是：" + tagsValue.getClass());
                }
            }
        } catch (Exception e) {
            // 处理 YAML 解析错误
            throw new IllegalArgumentException("解析 Front-matter (YAML) 出错: " + e.getMessage(), e);
        }        // --- 解析剩余内容部分，但只提取 saying，不提取 content ---
        int moreTagIndex = remainingContentPart.indexOf(MORE_TAG);

        if (moreTagIndex != -1) {
            // 找到了 <!-- more --> 标签
            // 标签之前的部分是 saying (需要 trim)
            String sayingPart = remainingContentPart.substring(0, moreTagIndex).trim();
            this.saying = sayingPart;
            
            // content将在需要时通过getContent()方法加载
            this.content = null;
        } else {
            // 没有找到 <!-- more --> 标签
            // saying为空，content将在需要时加载
            this.content = null;
            this.saying = null;
        }
    }
    public Path getFilepath() { return filepath; }
    public void setFilepath(Path filepath) { this.filepath = filepath; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getDate() { return dateTime; }
    public void setDate(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }
    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }
    public String getSaying() { return saying; }
    public void setSaying(String saying) { this.saying = saying; }    /**
     * 获取博客内容。如果内容尚未加载（为null）且filepath已设置，
     * 则会尝试从文件中读取内容。
     * 
     * @return 博客内容文本
     * @throws RuntimeException 如果读取文件失败
     */
    public String loadContent() { 
        if (content == null && filepath != null) {
            try {
                String fileContent = Files.readString(filepath);
                extractContentFromFile(fileContent);
            } catch (IOException e) {
                throw new RuntimeException("无法读取博客内容: " + filepath, e);
            }
        }
        return content; 
    }
    
    /**
     * 从文件内容中提取博客正文部分
     * 
     * @param fileContent 完整的文件内容
     */
    private void extractContentFromFile(String fileContent) {
        if (fileContent == null || !fileContent.startsWith(FRONT_MATTER_DELIMITER)) {
            return;
        }

        // 查找第二个分隔符的位置
        int secondDelimiterStart = fileContent.indexOf(FRONT_MATTER_DELIMITER + "\n", FRONT_MATTER_DELIMITER.length());
        if (secondDelimiterStart == -1) {
            secondDelimiterStart = fileContent.indexOf(FRONT_MATTER_DELIMITER + "\r\n", FRONT_MATTER_DELIMITER.length());
        }

        if (secondDelimiterStart == -1) {
            return;
        }

        // 提取内容部分 (分隔符之后的所有内容)
        String remainingContentPart = fileContent.substring(secondDelimiterStart + FRONT_MATTER_DELIMITER.length() + 1).trim();
        
        // --- 解析剩余内容部分 ---
        int moreTagIndex = remainingContentPart.indexOf(MORE_TAG);

        if (moreTagIndex != -1) {
            // 标签之后的部分是 content
            String contentPart = remainingContentPart.substring(moreTagIndex + MORE_TAG.length()).trim();
            this.content = contentPart;
        } else {
            // 没有找到 <!-- more --> 标签，全部作为 content
            this.content = remainingContentPart;
        }
    }
    
    public void setContent(String content) { this.content = content; }
    public String getContent() { return content; }
    
    
    public String Info() {
        if (this.saying != null){
            return "Blog{ \n" +
                    "---\n" +
                    "title: " + title + '\n' +
                    "date: " + (dateTime.toString()) + '\n' +
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
                    "date: " + (dateTime.toString()) + '\n' +
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
                    "date: " + (dateTime.toString()) + '\n' +
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
                    "date: " + (dateTime.toString()) + '\n' +
                    "categories: " + categories + '\n' +
                    "tags: " + Arrays.toString(tags) +'\n' + 
                    "---\n" +
                    (content != null ? content : "null") + '\n' +
                    '}';
        }
    }

    @Override
    public int compareTo(Blog other) {

        return other.dateTime.compareTo(this.dateTime);

    }
}
