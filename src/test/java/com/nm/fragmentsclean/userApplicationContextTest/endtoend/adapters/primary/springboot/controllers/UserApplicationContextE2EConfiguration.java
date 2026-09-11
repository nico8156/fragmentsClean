package com.nm.fragmentsclean.userApplicationContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.security.FakeCurrentUserProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CurrentUserProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class UserApplicationContextE2EConfiguration {
	@Primary
	@Bean
	public DateTimeProvider deterministicClockProvider() {
		return new DeterministicDateTimeProvider();
	}

	@Primary
	@Bean
	public CurrentUserProvider testCurrentUserProvider() {
		return new FakeCurrentUserProvider();
	}

	@Primary @Bean public PrivateImageStore testPrivateImageStore(){return new PrivateImageStore(){private void outsideTransaction(){if(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new AssertionError("Remote object storage call inside database transaction");}public UploadTarget presignUpload(String key,String type,Duration ttl,Instant at){outsideTransaction();return new UploadTarget(URI.create("https://upload.test/avatar"),"PUT",Map.of("Content-Type",type),at.plus(ttl));}public ProcessedImage normalize(String pending,String target,String type,ImageRules rules){outsideTransaction();return new ProcessedImage(target,"image/jpeg",400,512,512,"avatar-sha");}public URI presignDownload(String key,Duration ttl){outsideTransaction();return URI.create("https://download.test/"+key);}public void delete(String key){outsideTransaction();}};}
}
