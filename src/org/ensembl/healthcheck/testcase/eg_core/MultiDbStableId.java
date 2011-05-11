package org.ensembl.healthcheck.testcase.eg_core;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ensembl.healthcheck.DatabaseRegistry;
import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.DatabaseType;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.Team;
import org.ensembl.healthcheck.testcase.MultiDatabaseTestCase;
import org.ensembl.healthcheck.util.CollectionUtils;
import org.ensembl.healthcheck.util.ConnectionBasedSqlTemplateImpl;

/**
 * EG specific test that does not use species to
 * 
 * @author dstaines
 * 
 */
public class MultiDbStableId extends MultiDatabaseTestCase {

	public MultiDbStableId() {
		super();
		addAppliesToType(DatabaseType.CORE);
		setTeamResponsible(Team.ENSEMBL_GENOMES);
	}

	private final static String STABLE_ID = "select stable_id from %obj%_stable_id";
	private final static String[] OBJ_TYPES = { "gene", "transcript",
			"translation", "exon" };

	@Override
	public boolean run(DatabaseRegistry dbr) {
		boolean result = true;
		for (String objectType : OBJ_TYPES) {
			String query = STABLE_ID.replaceAll("%obj%", objectType);
			Map<String, Collection<String>> stableIds = CollectionUtils
					.createHashMap();
			for (DatabaseRegistryEntry coreDb : dbr.getAll(DatabaseType.CORE)) {
				ReportManager.info(this, coreDb.getConnection(), "Checking "
						+ objectType + " stable ID for " + coreDb.getName());
				ConnectionBasedSqlTemplateImpl template = new ConnectionBasedSqlTemplateImpl(
						coreDb.getConnection());
				int checked = 0;
				int dups = 0;
				for (String stableId : template.queryForDefaultObjectList(
						query, String.class)) {
					checked++;
					Collection<String> dbs = stableIds.get(stableId);
					if (dbs == null) {
						dbs = CollectionUtils.createArrayList(1);
						stableIds.put(stableId, dbs);
					} else {
						dups++;
						ReportManager
								.problem(
										this,
										coreDb.getConnection(),
										"The "
												+ objectType
												+ " stable ID has been found in the following core databases :"
												+ StringUtils.join(dbs, ", "));
						result = false;
					}
					dbs.add(coreDb.getName());
				}
				ReportManager.info(this, coreDb.getConnection(), "Checked "
						+ checked + " " + objectType + " stable IDs for "
						+ coreDb.getName() + ": found " + dups + " duplicates");
			}
		}
		return result;
	}

}