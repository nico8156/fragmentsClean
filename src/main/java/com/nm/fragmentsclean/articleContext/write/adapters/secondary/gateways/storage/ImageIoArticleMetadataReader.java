package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.storage;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleImageMetadataReader;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
@Component
public final class ImageIoArticleMetadataReader implements ArticleImageMetadataReader {
    public Dimensions read(byte[] bytes){
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
            if(stream==null)return new Dimensions(null,null);
            var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())return new Dimensions(null,null);
            var reader=readers.next();try{reader.setInput(stream,true,true);return new Dimensions(reader.getWidth(0),reader.getHeight(0));}finally{reader.dispose();}
        }catch(java.io.IOException | RuntimeException e){return new Dimensions(null,null);}
    }
}
