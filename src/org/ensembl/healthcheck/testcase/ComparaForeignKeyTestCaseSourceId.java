/*
  Copyright (C) 2003 EBI, GRL
 
  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.
 
  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
 
  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.ensembl.healthcheck.testcase;

import java.sql.*;

import org.ensembl.healthcheck.*;

import org.ensembl.healthcheck.util.*;

/**
 * An EnsEMBL Healthcheck test case that looks for broken foreign-key relationships.
 */

public class ComparaForeignKeyTestCaseSourceId extends EnsTestCase {
  
  /**
   * Create an OrphanTestCase that applies to a specific set of databases.
   */
  public ComparaForeignKeyTestCaseSourceId() {
      databaseRegexp = "^ensembl_compara_.*";
      addToGroup("compara_db_constraints");
      setDescription("Check for broken foreign-key relationships in ensembl_compara databases.");
  }
  
  public TestResult run() {
    
    boolean result = true;
    
    DatabaseConnectionIterator it = getDatabaseConnectionIterator();
    int orphans = 0;
    
    while (it.hasNext()) {
      
      Connection con = (Connection)it.next();

      if( getRowCount( con, "select count(*) from external_db" ) > 0 ) {

        orphans = countOrphans(con, "member", "source_id", "source", "source_id", true );
	if (orphans == 0) {
          ReportManager.correct(this, con, "source <- member relationships PASSED");
	} else if( orphans > 0 ) {
          ReportManager.problem(this, con, "source <- member has unlinked entries FAILED");
        } else {
          ReportManager.problem(this, con, "source <- member TEST NOT COMPLETED, look at the StackTrace if any");
        }

        orphans = countOrphans(con, "family", "source_id", "source", "source_id", true );
	if (orphans == 0) {
          ReportManager.correct(this, con, "source <- family relationships PASSED");
	} else if( orphans > 0 ) {
          ReportManager.problem(this, con, "source <- family has unlinked entries FAILED");
        } else {
          ReportManager.problem(this, con, "source <- family TEST NOT COMPLETED, look at the StackTrace if any");
        }

        orphans = countOrphans(con, "homology", "source_id", "source", "source_id", true );
	if (orphans == 0) {
          ReportManager.correct(this, con, "source <- homology relationships PASSED");
	} else if( orphans > 0 ) {
          ReportManager.problem(this, con, "source <- homology has unlinked entries FAILED");
        } else {
          ReportManager.problem(this, con, "source <- homology TEST NOT COMPLETED, look at the StackTrace if any");
        }

        orphans = countOrphans(con, "domain", "source_id", "source", "source_id", true );
	if (orphans == 0) {
          ReportManager.correct(this, con, "source <- domain relationships PASSED");
	} else if( orphans > 0 ) {
          ReportManager.problem(this, con, "source <- domain has unlinked entries FAILED");
        } else {
          ReportManager.problem(this, con, "source <- domain TEST NOT COMPLETED, look at the StackTrace if any");
        }

      } else {
	  ReportManager.correct(this, con, "NO ENTRIES in source table, so nothing to test IGNORED");
      }
      
      result &= (orphans == 0);

    }
    
    return new TestResult(getShortTestName(), result);
    
  }
  
} // OrphanTestCase
