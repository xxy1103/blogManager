package com.ulna.blog_manager.model;

import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList; // Needed for tags conversion
import java.util.Arrays;

public class BlogParser {

    private static final String FRONT_MATTER_DELIMITER = "---";
    private static final String MORE_TAG = "<!-- more -->";

    /**
     * 解析包含 Hexo Front-matter 的 Markdown 文件内容。
     *
     * @param fileContent 文件的完整字符串内容。
     * @return 解析后的 Blog 对象。
     * @throws IllegalArgumentException 如果文件格式不正确（缺少分隔符等）。
     */
    public Blog parseBlogContent(String fileContent) {
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

        Blog blog = new Blog();

        // --- 解析 YAML Front-matter ---
        Yaml yaml = new Yaml();
        try {
            Map<String, Object> frontMatter = yaml.load(yamlPart);

            if (frontMatter != null) {
                blog.setTitle((String) frontMatter.get("title"));
                // 日期可能是 Date 对象或 String，这里先统一转为 String
                Object dateValue = frontMatter.get("date");
                blog.setDate(dateValue != null ? dateValue.toString() : null);
                blog.setCategories((String) frontMatter.get("categories"));

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
                     blog.setTags(stringTags.toArray(new String[0]));

                } else if (tagsValue != null) {
                     // 如果不是 List 但存在，尝试转为 String 并放入单元素数组（或记录警告）
                     blog.setTags(new String[]{tagsValue.toString()});
                     System.err.println("警告：Tags 格式可能不正确，期望是列表，实际是：" + tagsValue.getClass());
                }
            }
        } catch (Exception e) {
            // 处理 YAML 解析错误
            throw new IllegalArgumentException("解析 Front-matter (YAML) 出错: " + e.getMessage(), e);
        }


        // --- 解析剩余内容部分 ---
        int moreTagIndex = remainingContentPart.indexOf(MORE_TAG);

        if (moreTagIndex != -1) {
            // 找到了 <!-- more --> 标签
            // 标签之前的部分是 saying (需要 trim)
            String sayingPart = remainingContentPart.substring(0, moreTagIndex).trim();
            blog.setSaying(sayingPart);

            // 标签之后的部分是 content (需要 trim)
            // + MORE_TAG.length() 是为了跳过标签本身
            String contentPart = remainingContentPart.substring(moreTagIndex + MORE_TAG.length()).trim();
            blog.setContent(contentPart);
        } else {
            // 没有找到 <!-- more --> 标签
            // 按照示例，第一行非空内容可能是 saying，其余是 content
            // 或者，更简单的处理方式是：整个 remainingContentPart 作为 content，saying 为空
            // 这里采用简单方式：全部作为 content
            blog.setContent(remainingContentPart);
            blog.setSaying(null); // 或者设置为 ""

            // --- 备选方案：尝试提取第一行作为 saying ---
            // String[] lines = remainingContentPart.split("\\R", 2); // \\R 匹配任何换行符, 限制为2部分
            // if (lines.length > 0 && !lines[0].trim().isEmpty()) {
            //     blog.setSaying(lines[0].trim());
            //     if (lines.length > 1) {
            //         blog.setContent(lines[1].trim());
            //     } else {
            //         blog.setContent(""); // 如果只有一行，则 content 为空
            //     }
            // } else {
            //     blog.setContent(remainingContentPart); // 如果内容为空，则 content 也是空
            //     blog.setSaying(null);
            // }
            // --- 结束备选方案 ---
        }

        return blog;
    }

    // --- 主方法示例：如何使用 ---
    public static void main(String[] args) {
        // 模拟从文件读取的内容
        String fileContent = "---\n" +
                "title: Maven-构建生命周期\n" +
                "date: 2025-05-05 10:33:33\n" +
                "categories: 笔记\n" +
                "tags: ['java','maven']\n" + // YAML 列表格式
                // 或者 tags:\n - java\n - maven\n
                "---\n" +
                "\n" + // YAML 后的空行
                "saying\n" + // 谚语
                "\n" + // 谚语后的空行
                "<!-- more -->\n" + // more 标签
                "\n" + // more 标签后的空行
                "正文\n" + // 正文内容
                "这是第二行正文。\n";

        BlogParser parser = new BlogParser();
        try {
            Blog blogPost = parser.parseBlogContent(fileContent);
            System.out.println("解析成功:");
            System.out.println(blogPost);

            System.out.println("\n详细属性:");
            System.out.println("Title: " + blogPost.getTitle());
            System.out.println("Date: " + blogPost.getDate());
            System.out.println("Categories: " + blogPost.getCategories());
            System.out.println("Tags: " + Arrays.toString(blogPost.getTags()));
            System.out.println("Saying: " + blogPost.getSaying());
            System.out.println("Content:\n" + blogPost.getContent());

        } catch (IllegalArgumentException e) {
            System.err.println("解析失败: " + e.getMessage());
            e.printStackTrace();
        }

         System.out.println("\n--- 测试无 more 标签 ---");
         String fileContentNoMore = "---\n" +
                 "title: 无More标签测试\n" +
                 "date: 2025-05-06 09:00:00\n" +
                 "categories: 测试\n" +
                 "tags: [test]\n" +
                 "---\n" +
                 "\n" +
                 "这是唯一的正文内容。\n" +
                 "它应该全部进入 content 字段。";
         try {
             Blog blogPostNoMore = parser.parseBlogContent(fileContentNoMore);
             System.out.println("解析成功:");
             System.out.println(blogPostNoMore);
             System.out.println("Saying: " + blogPostNoMore.getSaying()); // 应为 null
             System.out.println("Content:\n" + blogPostNoMore.getContent());
         } catch (IllegalArgumentException e) {
             System.err.println("解析失败: " + e.getMessage());
         }
    }
}
