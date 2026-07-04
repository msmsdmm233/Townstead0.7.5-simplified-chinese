package com.aetherianartificer.townstead.root.attachment;

import com.aetherianartificer.townstead.pheno.lang.schema.NodeSchema;
import com.aetherianartificer.townstead.pheno.lang.schema.PhenoType;
import com.aetherianartificer.townstead.pheno.lang.validate.NodeDomain;

import static com.aetherianartificer.townstead.pheno.lang.schema.FieldSchema.of;
import static com.aetherianartificer.townstead.pheno.lang.schema.FieldSchema.required;

/**
 * Field schemas for the attachment data files, validated through the shared schema/diagnostics
 * engine so a malformed file is a located error rather than a silent skip. Plain data records
 * (no behavior nodes), so they sit in the {@link NodeDomain#DATA} domain and are validated by
 * {@code PhenoValidator.validateData} without any behavior-tree descent.
 */
public final class AttachmentSchemas {

    private AttachmentSchemas() {}

    public static final NodeSchema ATTACHMENT = NodeSchema.of("townstead:attachment", NodeDomain.DATA)
            .doc("A cosmetic geometry attached to a body (tail, tusk, ears).")
            .field(of("schema", PhenoType.STRING).doc("Schema tag: townstead:attachment/v1."))
            .field(required("geometry", PhenoType.STRING).doc("Reference to a .geo.json under attachment/geo/."))
            .field(required("texture", PhenoType.STRING).doc("Reference to a .png under attachment/textures/."))
            .field(of("target", PhenoType.OBJECT).doc("{ tag } (every matching point) or { point } (one point)."))
            .field(of("bone", PhenoType.STRING).doc("Direct bone anchor when there is no target."))
            .field(of("offset", PhenoType.FLOAT).asList())
            .field(of("rotation", PhenoType.FLOAT).asList())
            .field(of("scale", PhenoType.FLOAT))
            .field(of("tint", PhenoType.COLOR))
            .field(of("skin_tint", PhenoType.BOOL)
                    .doc("Tint by the bearer's resolved skin tone instead of the flat tint (grayscale texture)."))
            .field(of("morph", PhenoType.OBJECT)
                    .doc("{ axes, bones }: scale by the size value rolled on the granting gene; "
                            + "`axes` weights the value per axis, `bones` names the top-level geometry bones "
                            + "scaled about their own pivots."))
            .build();

    public static final NodeSchema ATTACHMENT_POINT = NodeSchema.of("townstead:attachment_point", NodeDomain.DATA)
            .doc("A named anchor on a rig where attachments may sit.")
            .field(of("schema", PhenoType.STRING).doc("Schema tag: townstead:attachment_point/v1."))
            .field(of("bone", PhenoType.STRING))
            .field(of("offset", PhenoType.FLOAT).asList())
            .field(of("tags", PhenoType.STRING).asList().doc("Tags attachments target (ear, tail_root, ...)."))
            .build();
}
