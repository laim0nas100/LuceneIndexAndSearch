package lt.lb.luceneindexandsearch;

import java.io.IOException;
import java.util.Objects;
import lt.lb.commons.LineStringBuilder;
import lt.lb.commons.func.Lambda;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.IndexReader;

/**
 *
 * @author laim0nas100
 */
public class LuceneUtil {

    public static boolean isClosed(IndexReader reader) throws IOException {
        return (reader == null || reader.getRefCount() == 0);
    }

    public static void appendStatus(boolean verbose, LineStringBuilder lines, CheckIndex.Status stat) {
        if (verbose) {
            appendStatusExtended(" ", lines, stat);
        } else {
            appendStatus(" ", lines, stat);
        }
    }

    public static void appendStatus(String tab, LineStringBuilder lines, CheckIndex.Status stat) {
        if (stat == null) {
            return;
        }
        lines.appendLine(tab, "directory", "=", stat.dir);
        lines.appendLine(tab, "clean", "=", stat.clean);
        lines.appendLine(tab, "maxSegmentName", "=", stat.maxSegmentName);
        lines.appendLine(tab, "missingSegments", "=", stat.missingSegments);
        lines.appendLine(tab, "numBadSegments", "=", stat.numBadSegments);
        lines.appendLine(tab, "numSegments", "=", stat.numSegments);
        lines.appendLine(tab, "partial", "=", stat.partial);
        lines.appendLine(tab, "segmentsChecked", "=", stat.segmentsChecked);
        lines.appendLine(tab, "segmentsFileName", "=", stat.segmentsFileName);
        lines.appendLine(tab, "toolOutOfDate", "=", stat.toolOutOfDate);
        lines.appendLine(tab, "validCounter", "=", stat.validCounter);
        lines.appendLine(tab, "segmentsChecked", "=", stat.segmentsChecked);
    }

    public static void appendStatusExtended(String tab, LineStringBuilder lines, CheckIndex.Status stat) {
        if (stat == null) {
            return;
        }
        appendStatus(tab, lines, stat);
        lines.appendLine(tab, "segmentInfos", "={");
        boolean first = true;
        for (CheckIndex.Status.SegmentInfoStatus sis : stat.segmentInfos) {

            if (first) {
                first = false;
            } else {
                lines.append(", ");
            }
            appendSegmentInfoStatus(tab, lines, sis);
        }
        lines.append("}");
    }

    public static void appendSegmentInfoStatus(String tab, LineStringBuilder lines, CheckIndex.Status.SegmentInfoStatus stat) {
        if (stat == null) {
            return;
        }
        lines.appendLine(tab, "codec", "=", stat.codec);
        lines.appendLine(tab, "compound", "=", stat.compound);
        lines.appendLine(tab, "deletionsGen", "=", stat.deletionsGen);
        lines.appendLine(tab, "diagnostics", "=", stat.diagnostics);
        lines.appendLine(tab, "hasDeletions", "=", stat.hasDeletions);
        lines.appendLine(tab, "maxDoc", "=", stat.maxDoc);
        lines.appendLine(tab, "name", "=", stat.name);
        lines.appendLine(tab, "numFiles", "=", stat.numFiles);
        lines.appendLine(tab, "openReaderPassed", "=", stat.openReaderPassed);
        lines.appendLine(tab, "sizeMB", "=", stat.sizeMB);

        appendFieldNested(tab, "docValuesStatus", lines, stat.docValuesStatus, LuceneUtil::appendDocValuesStatus);

        appendFieldNested(tab, "fieldInfoStatus", lines, stat.fieldInfoStatus, LuceneUtil::appendFieldInfoStatus);

        appendFieldNested(tab, "fieldNormStatus", lines, stat.fieldNormStatus, LuceneUtil::appendFieldNormStatus);

        appendFieldNested(tab, "indexSortStatus", lines, stat.indexSortStatus, LuceneUtil::appendIndexSortStatus);

        appendFieldNested(tab, "liveDocStatus", lines, stat.liveDocStatus, LuceneUtil::appendLiveDocStatus);

        appendFieldNested(tab, "pointsStatus", lines, stat.pointsStatus, LuceneUtil::appendPointsStatus);

        appendFieldNested(tab, "storedFieldStatus", lines, stat.storedFieldStatus, LuceneUtil::appendStoredFieldStatus);

        appendFieldNested(tab, "termIndexStatus", lines, stat.termIndexStatus, LuceneUtil::appendTermIndexStatus);

        appendFieldNested(tab, "termVectorStatus", lines, stat.termVectorStatus, LuceneUtil::appendTermVectorStatus);

    }

    public static <T> void appendFieldNested(String tab, String field, LineStringBuilder lines, T item,
            Lambda.L3<String, LineStringBuilder, T> printer
    ) {
        if (item == null) {
            lines.appendLine(tab, field, "=null");
            return;
        }

        Objects.requireNonNull(printer, "Printer is null");
        lines.appendLine(tab, field, "={");
        printer.apply(tab+" ", lines, item);
        lines.appendLine(tab, "}");
    }

    public static void appendDocValuesStatus(String tab, LineStringBuilder lines, CheckIndex.Status.DocValuesStatus dvs) {
        if (dvs == null) {
            return;
        }
        lines
                .appendLine(tab, "error", "=", dvs.error)
                .appendLine(tab, "totalBinaryFields", "=", dvs.totalBinaryFields)
                .appendLine(tab, "totalNumericFields", "=", dvs.totalNumericFields)
                .appendLine(tab, "totalSortedFields", "=", dvs.totalSortedFields)
                .appendLine(tab, "totalSortedNumericFields", "=", dvs.totalSortedNumericFields)
                .appendLine(tab, "totalSortedSetFields", "=", dvs.totalSortedSetFields)
                .appendLine(tab, "totalValueFields", "=", dvs.totalValueFields);
    }

    public static void appendFieldInfoStatus(String tab, LineStringBuilder lines, CheckIndex.Status.FieldInfoStatus fis) {
        if (fis == null) {
            return;
        }
        lines
                .appendLine(tab, "error", "=", fis.error)
                .appendLine(tab, "totFields", "=", fis.totFields);
    }

    public static void appendFieldNormStatus(String tab, LineStringBuilder lines, CheckIndex.Status.FieldNormStatus fis) {
        if (fis == null) {
            return;
        }
        lines
                .appendLine(tab, "error", "=", fis.error)
                .appendLine(tab, "totFields", "=", fis.totFields);
    }

    public static void appendIndexSortStatus(String tab, LineStringBuilder lines, CheckIndex.Status.IndexSortStatus iss) {
        if (iss == null) {
            return;
        }
        lines
                .appendLine(tab, "error", "=", iss.error);
    }

    public static void appendLiveDocStatus(String tab, LineStringBuilder lines, CheckIndex.Status.LiveDocStatus lds) {
        if (lds == null) {
            return;
        }
        lines
                .appendLine(tab, "error", "=", lds.error)
                .appendLine(tab, "numDeleted", "=", lds.numDeleted);
    }

    public static void appendPointsStatus(String tab, LineStringBuilder lines, CheckIndex.Status.PointsStatus ps) {
        if (ps == null) {
            return;
        }
        lines
                .appendLine(tab, "error", "=", ps.error)
                .appendLine(tab, "totalValueFields", "=", ps.totalValueFields)
                .appendLine(tab, "totalValuePoints", "=", ps.totalValuePoints);
    }

    public static void appendStoredFieldStatus(String tab, LineStringBuilder lines, CheckIndex.Status.StoredFieldStatus ps) {
        if (ps == null) {
            return;
        }
        lines
                .appendLine(tab, "error", "=", ps.error)
                .appendLine(tab, "docCount", "=", ps.docCount)
                .appendLine(tab, "totFields", "=", ps.totFields);
    }

    public static void appendTermIndexStatus(String tab, LineStringBuilder lines, CheckIndex.Status.TermIndexStatus tis) {
        if (tis == null) {
            return;
        }
        lines
                .appendLine(tab, "error", "=", tis.error)
                .appendLine(tab, "delTermCount", "=", tis.delTermCount)
                .appendLine(tab, "termCount", "=", tis.termCount)
                .appendLine(tab, "totFreq", "=", tis.totFreq)
                .appendLine(tab, "totPos", "=", tis.totPos)
                .appendLine(tab, "blockTreeStats", "=", tis.blockTreeStats);
    }

    public static void appendTermVectorStatus(String tab, LineStringBuilder lines, CheckIndex.Status.TermVectorStatus tvs) {
        if (tvs == null) {
            return;
        }
        lines
                .appendLine(tab, "error", "=", tvs.error)
                .appendLine(tab, "docCount", "=", tvs.docCount)
                .appendLine(tab, "totVectors", "=", tvs.totVectors);
    }

}
