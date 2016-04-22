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

package com.flipkart.flux.rules;

import com.flipkart.flux.domain.AuditRecord;
import com.flipkart.flux.domain.Event;
import com.flipkart.flux.domain.State;
import com.flipkart.flux.domain.StateMachine;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.junit.rules.ExternalResource;

import javax.inject.Inject;

/**
 * @author shyam.akirala
 */
public class DbClearRule extends ExternalResource{

    @Inject
    private SessionFactory sessionFactory;

    private static Class[] tables = {StateMachine.class, State.class, AuditRecord.class, Event.class};

    @Override
    protected void before() throws Throwable {
        super.before();
        clearDb();
    }

    private void clearDb() {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);
        Transaction tx = session.beginTransaction();
        try {

            sessionFactory.getCurrentSession().createSQLQuery("set foreign_key_checks=0").executeUpdate();

            for (Class anEntity : tables) {
                sessionFactory.getCurrentSession().createSQLQuery("delete from " + anEntity.getSimpleName() + "s").executeUpdate();
            }

            sessionFactory.getCurrentSession().createSQLQuery("set foreign_key_checks=1").executeUpdate();
            tx.commit();
        } finally {
            if(session != null) {
                ManagedSessionContext.unbind(sessionFactory);
                session.close();
            }

        }
    }
}