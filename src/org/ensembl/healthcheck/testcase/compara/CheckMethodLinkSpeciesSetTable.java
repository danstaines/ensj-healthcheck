/*
 * Copyright [1999-2015] Wellcome Trust Sanger Institute and the EMBL-European Bioinformatics Institute
 * Copyright [2016-2018] EMBL-European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.ensembl.healthcheck.testcase.compara;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.Team;
import org.ensembl.healthcheck.testcase.compara.AbstractComparaTestCase;
import org.ensembl.healthcheck.util.DBUtils;

/**
 * An EnsEMBL Healthcheck test case that looks for broken entries in the
 * method_link_species_set table
 */

public class CheckMethodLinkSpeciesSetTable extends AbstractComparaTestCase {

	public CheckMethodLinkSpeciesSetTable() {
		setDescription("Check for broken entries in the method_link_species_set table.");
		setTeamResponsible(Team.COMPARA);
	}

	public boolean run(DatabaseRegistryEntry dbre) {

		Connection con = dbre.getConnection();

		boolean result = true;

		/* Check number of MLSS with no source */
		result &= checkCountIsZero(con, "method_link_species_set", "source = 'NULL' OR source IS NULL");

		/* Check number of MLSS with no name */
		result &= checkCountIsZero(con, "method_link_species_set", "name = 'NULL' OR name IS NULL");

		/* Check the genomes in the species_set linked to the MLSS table */
		int numOfGenomesInTheDatabase = DBUtils.getRowCount(con, "SELECT count(*) FROM genome_db WHERE taxon_id > 0");
		Pattern unaryPattern = Pattern.compile("^([A-Z]\\.[a-z0-9]{2,3}) ");
		Pattern binaryPattern = Pattern.compile("^([A-Z]\\.[a-z0-9]{2,3})-([A-Z]\\.[a-z0-9]{2,3})");
		Pattern multiPattern = Pattern.compile("([0-9]+)");
		Pattern ssnamePattern = Pattern.compile("^([a-zA-Z]+) ");
		/* Query returns the MLLS.name, the number of genomes and their name ("H.sap" format) */
		String sql = "SELECT method_link_species_set.name, count(*),"+
			" GROUP_CONCAT( CONCAT( UPPER(substr(genome_db.name, 1, 1)), '.', SUBSTR(SUBSTRING_INDEX(genome_db.name, '_', -1),1,3) ) ), "+
			" species_set_id, species_set_header.name, "+
			" method_link_species_set_id "+
			" FROM method_link_species_set JOIN species_set USING (species_set_id)"+
			" JOIN species_set_header USING (species_set_id)"+
			" JOIN genome_db USING (genome_db_id) GROUP BY method_link_species_set_id";
		try {
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs != null) {
				while (rs.next()) {
					String name = rs.getString(1);
					int num = rs.getInt(2);
					String genomes = rs.getString(3);
					String ss_id = rs.getString(4);
					String ss_name = rs.getString(5);
					String mlss_id = rs.getString(6);
					Matcher unaryMatcher = unaryPattern.matcher(name);
					Matcher binaryMatcher = binaryPattern.matcher(name);
					Matcher multiMatcher = multiPattern.matcher(name);
					Matcher ssnameMatcher = ssnamePattern.matcher(name);
					if (unaryMatcher.find()) {
						if (num != 1) {
							ReportManager.problem(this, con, "FAILED species_set(" + ss_id + ") for \"" + name + "\"(" + mlss_id + ") links to " + num + " genomes instead of 1");
							result = false;
						}
						if (!genomes.equals(unaryMatcher.group(1))) {
							ReportManager.problem(this, con, "FAILED species_set(" + ss_id + ") for \"" + name + "\"(" + mlss_id + ") links to " + genomes);
						}
					} else if (binaryMatcher.find()) {
						if (num != 2 && binaryMatcher.group(1) != binaryMatcher.group(2)) {
							ReportManager.problem(this, con, "FAILED species_set(" + ss_id + ") for \"" + name + "\"(" + mlss_id + ") links to " + num + " genomes instead of 2");
							result = false;
						}
						if (!genomes.equals(binaryMatcher.group(1)+ "," + binaryMatcher.group(2)) && !genomes.equals(binaryMatcher.group(2) + "," + binaryMatcher.group(1))) {
							if (binaryMatcher.group(1).equals (binaryMatcher.group(2)) && genomes.equals(binaryMatcher.group(1))) {
							} else {
								ReportManager.problem(this, con, "Yes, we felt here... FAILED species_set(" + ss_id + ") for \"" + name + "\"(" + mlss_id + ") links to " + genomes + " instead of " + binaryMatcher.group(1) + "," + binaryMatcher.group(2));
							}
						}
					} else if (multiMatcher.find()) {
						if (num != Integer.valueOf(multiMatcher.group()).intValue()) {
							ReportManager.problem(this, con, "FAILED species_set(" + ss_id + ") for \"" + name + "\"(" + mlss_id + ") links to " + num + " genomes instead of " + multiMatcher.group());
							result = false;
						}
					} else if (ssnameMatcher.find()) {
						if (ssnameMatcher.group(1).equals("protein") || ssnameMatcher.group(1).equals("nc") || ssnameMatcher.group(1).equals("species")) {
							ReportManager.info(this, con, "\"" + name + "\"(" + mlss_id + ") is named using the old convention (the collection name is missing)");
						} else if (!ss_name.equals("collection-" + ssnameMatcher.group(1)) && !ss_name.equals(ssnameMatcher.group(1))) {
							ReportManager.problem(this, con, "FAILED species_set(" + ss_id + ") for \"" + name + "\"(" + mlss_id + ") does not start with the species-set name " + ss_name);
							result = false;
						}
					} else if (num != numOfGenomesInTheDatabase && !isMasterDB(dbre.getConnection())) {
						ReportManager.problem(this, con, "FAILED species_set(" + ss_id + ") for \"" + name + "\"(" + mlss_id + ") links to " + num + " genomes instead of " + numOfGenomesInTheDatabase);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

} // CheckMethodLinkSpeciesSetTable
