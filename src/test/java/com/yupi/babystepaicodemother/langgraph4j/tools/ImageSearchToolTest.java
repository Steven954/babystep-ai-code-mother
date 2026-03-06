package com.yupi.babystepaicodemother.langgraph4j.tools;

import com.yupi.babystepaicodemother.langgraph4j.model.ImageResource;
import com.yupi.babystepaicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
    void testSearchContentImages() {
        // 测试正常搜索
        List<ImageResource> images = imageSearchTool.searchContentImages("technology");
        assertNotNull(images);
        assertFalse(images.isEmpty());
        // 验证返回的图片资�?
        ImageResource firstImage = images.get(0);
        assertEquals(ImageCategoryEnum.CONTENT, firstImage.getCategory());
        assertNotNull(firstImage.getDescription());
        assertNotNull(firstImage.getUrl());
        assertTrue(firstImage.getUrl().startsWith("http"));
        System.out.println("搜索�?" + images.size() + " 张图�?);
        images.forEach(image ->
                System.out.println("图片: " + image.getDescription() + " - " + image.getUrl())
        );
    }
}
