ALTER TABLE public.venta_pago
    ADD COLUMN IF NOT EXISTS motivo_rechazo TEXT NULL;
