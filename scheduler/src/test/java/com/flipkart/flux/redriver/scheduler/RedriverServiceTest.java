/*
 * Copyright 2012-2016, the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.flux.redriver.scheduler;

import com.flipkart.flux.redriver.model.ScheduledMessage;
import com.flipkart.flux.redriver.service.MessageManagerService;
import com.flipkart.flux.redriver.service.RedriverService;
import com.flipkart.flux.task.redriver.RedriverRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RedriverServiceTest {
    @Mock
    MessageManagerService messageManagerService;

    @Mock
    RedriverRegistry redriverRegistry;

    private RedriverService redriverService;

    private  int batchSize = 2;

    @Before
    public void setUp() throws Exception {
        redriverService = new RedriverService(messageManagerService, redriverRegistry, 500, batchSize);
        redriverService.setInitialDelay(0L);
    }

    @Test
    public void testIdle() throws Exception {
        Thread.sleep(200);
        Assert.assertFalse(redriverService.isRunning());
        verifyZeroInteractions(messageManagerService);
        verifyZeroInteractions(redriverRegistry);
    }

    @Test
    public void testRedriveMessage_shouldRedriveWhenOldMessageFound() throws Exception {
        long now = System.currentTimeMillis();
        when(messageManagerService.retrieveOldest(0, batchSize)).
                thenReturn(Arrays.asList(new ScheduledMessage(1l, now - 2), new ScheduledMessage(2l, now + 10000))).
                thenReturn(Arrays.asList(new ScheduledMessage(2l, now + 10000)));
        
        redriverService.start();

        Thread.sleep(300);
        verify(redriverRegistry).redriveTask(1l);
        Thread.sleep(500);
        verifyNoMoreInteractions(redriverRegistry);
    }

    @Test
    public void testLargeNoOfRedriveMessages_allShouldBeRedrived() throws Exception {
        ArrayList<ScheduledMessage> scheduledMessages  = new ArrayList<>();
        int batchSize  = 100;
        long now = System.currentTimeMillis();
        for(long i = 0 ; i < 1000; i++)
            scheduledMessages.add(new ScheduledMessage(i,now-100));
        redriverService = new RedriverService(messageManagerService, redriverRegistry, 100, batchSize);
        redriverService.setInitialDelay(0L);
        for(int i = 0 ; i < 10 ; i++)
            when(messageManagerService.retrieveOldest(i*100, batchSize)).thenReturn(scheduledMessages.subList(i*100, (i+1)*100)).thenReturn(null);
        redriverService.start();
        Thread.sleep(1000);
        redriverService.stop();
        for(long i = 0 ; i < 100; i++) {
            long x = (long)(Math.random()*1000.0);
            verify(redriverRegistry).redriveTask(x);
        }
    }

    @Test
    public void testNoRedriveMessages_shouldNotRedriveMessagesWithScheduledTimeOfFuture() throws Exception {
        long now = System.currentTimeMillis();
        when(messageManagerService.retrieveOldest(0, batchSize)).
                thenReturn(Arrays.asList(new ScheduledMessage(1l, now + 9000), new ScheduledMessage(2l, now + 10000)));

        redriverService.start();

        Thread.sleep(1000);
        verifyZeroInteractions(redriverRegistry);
    }

    @Test
    public void testStartStopCycle() throws Exception {
        when(messageManagerService.retrieveOldest(0, batchSize)).
                thenReturn(Arrays.asList(new ScheduledMessage(3l, 1l))).
                thenReturn(Arrays.asList(new ScheduledMessage(4l, 2l)));

        redriverService.start();
        Thread.sleep(100);
        Assert.assertTrue(redriverService.isRunning());
        verify(redriverRegistry).redriveTask(3l);

        redriverService.stop();
        Thread.sleep(600);
        Assert.assertFalse(redriverService.isRunning());
        verifyNoMoreInteractions(redriverRegistry);

        redriverService.start();
        Thread.sleep(100);

        verify(redriverRegistry).redriveTask(4l);
        verifyNoMoreInteractions(redriverRegistry);
    }
}